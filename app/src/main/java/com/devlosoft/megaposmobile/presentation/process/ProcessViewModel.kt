package com.devlosoft.megaposmobile.presentation.process

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.dataphone.DataphoneManager
import com.devlosoft.megaposmobile.core.printer.LocalPrintTemplates
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.core.state.DataphoneState
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.data.remote.dto.DataphoneDataDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.repository.PaymentRepository
import com.devlosoft.megaposmobile.domain.usecase.CloseTerminalUseCase
import com.devlosoft.megaposmobile.domain.usecase.GetSessionInfoUseCase
import com.devlosoft.megaposmobile.domain.usecase.OpenTerminalUseCase
import com.devlosoft.megaposmobile.domain.usecase.PrintDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

object ProcessTypes {
    const val OPEN_TERMINAL = "openTerminal"
    const val CLOSE_TERMINAL = "closeTerminal"
    const val CLOSE_DATAPHONE = "closeDataphone"
    const val FINALIZE_PAYMENT = "finalizePayment"
}

@HiltViewModel
class ProcessViewModel @Inject constructor(
    private val openTerminalUseCase: OpenTerminalUseCase,
    private val closeTerminalUseCase: CloseTerminalUseCase,
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager,
    private val stationStatus: StationStatus,
    private val printDocumentsUseCase: PrintDocumentsUseCase,
    private val getSessionInfoUseCase: GetSessionInfoUseCase,
    private val dataphoneManager: DataphoneManager,
    private val dataphoneState: DataphoneState,
    private val serverConfigDao: ServerConfigDao,
    private val paymentRepository: PaymentRepository,
    private val printerManager: PrinterManager
) : ViewModel() {

    companion object {
        private const val TAG = "ProcessViewModel"
    }

    private val _state = MutableStateFlow(ProcessState())
    val state: StateFlow<ProcessState> = _state.asStateFlow()

    fun startProcess(processType: String) {
        when (processType) {
            ProcessTypes.OPEN_TERMINAL -> openTerminal()
            ProcessTypes.CLOSE_TERMINAL -> closeTerminal()
            ProcessTypes.CLOSE_DATAPHONE -> closeDataphone()
            else -> {
                _state.update {
                    it.copy(status = ProcessStatus.Error("Tipo de proceso desconocido"))
                }
            }
        }
    }

    fun startPaymentProcess(transactionId: String, amount: Double) {
        viewModelScope.launch {
            Log.d(TAG, "startPaymentProcess() called - transactionId: $transactionId, amount: $amount")

            // Format amount as currency
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
                maximumFractionDigits = 0
            }
            val formattedAmount = numberFormat.format(amount)

            // Set loading state with payment message
            _state.update {
                it.copy(
                    status = ProcessStatus.Loading,
                    loadingMessage = "Favor realice el pago en el datáfono\npor el monto de $formattedAmount"
                )
            }

            // Call dataphone for real payment
            val paymentResult = dataphoneManager.processPayment(amount.toLong())

            paymentResult.fold(
                onSuccess = { dataphoneResult ->
                    Log.d(TAG, "Payment successful: auth=${dataphoneResult.autorizacion}")

                    // Update terminal ID if it's new or different
                    val newTerminalId = dataphoneResult.terminalid ?: ""
                    if (newTerminalId.isNotBlank()) {
                        val currentTerminalId = dataphoneState.getTerminalId()
                        if (currentTerminalId.isBlank() || currentTerminalId != newTerminalId) {
                            Log.d(TAG, "Terminal ID changed: '$currentTerminalId' -> '$newTerminalId'")
                            serverConfigDao.updateDataphoneTerminalId(newTerminalId)
                            dataphoneState.setTerminalId(newTerminalId)
                        }
                    }

                    // Get session data using UseCase
                    val sessionResult = getSessionInfoUseCase()
                    if (sessionResult.isFailure) {
                        _state.update {
                            it.copy(status = ProcessStatus.Error(sessionResult.exceptionOrNull()?.message ?: "No hay sesión activa"))
                        }
                        return@launch
                    }

                    val sessionInfo = sessionResult.getOrThrow()

                    // Create DTO with dataphone data
                    val dataphoneData = DataphoneDataDto(
                        autorizacion = dataphoneResult.autorizacion,
                        panmasked = dataphoneResult.panmasked,
                        cardholder = dataphoneResult.cardholder,
                        terminalid = dataphoneResult.terminalid ?: "",
                        recibo = dataphoneResult.recibo,
                        rrn = dataphoneResult.rrn,
                        stan = dataphoneResult.stan,
                        ticket = dataphoneResult.ticket,
                        totalAmount = dataphoneResult.totalAmount
                    )

                    // Call finalize transaction with real dataphone data
                    Log.d(TAG, "Calling finalizeTransaction with dataphone data...")
                    billingRepository.finalizeTransaction(
                        sessionId = sessionInfo.sessionId,
                        workstationId = sessionInfo.stationId,
                        transactionId = transactionId,
                        dataphoneData = dataphoneData
                    ).collect { result ->
                        when (result) {
                            is Resource.Loading -> {
                                // Already in loading state
                            }
                            is Resource.Success -> {
                                Log.d(TAG, "Transaction finalized successfully, now printing...")
                                // Transaction finalized successfully, now fetch and print documents
                                fetchAndPrintDocuments(transactionId)
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "Error finalizing transaction: ${result.message}")
                                _state.update {
                                    it.copy(status = ProcessStatus.Error(result.message ?: "Error al finalizar transacción"))
                                }
                            }
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Payment failed", error)
                    _state.update {
                        it.copy(status = ProcessStatus.Error(
                            error.message ?: "Error al procesar pago en datáfono"
                        ))
                    }
                }
            )
        }
    }

    private suspend fun fetchAndPrintDocuments(transactionId: String) {
        Log.d(TAG, "fetchAndPrintDocuments() called for transaction: $transactionId")

        _state.update {
            it.copy(
                isPrinting = true,
                pendingPrintTransactionCode = transactionId
            )
        }

        printDocumentsUseCase(transactionId)
            .onSuccess { printedCount ->
                Log.d(TAG, "Successfully printed $printedCount documents")
                _state.update {
                    it.copy(
                        status = ProcessStatus.Success("La transacción fue cerrada con éxito"),
                        isPrinting = false,
                        pendingPrintTransactionCode = null
                    )
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Error printing documents: ${error.message}")
                // Show print error with retry/skip options
                _state.update {
                    it.copy(
                        status = ProcessStatus.PrintError(
                            error.message ?: "Error al imprimir los documentos"
                        ),
                        isPrinting = false
                        // Keep pendingPrintTransactionCode for retry
                    )
                }
            }
    }

    fun retryPrint() {
        val transactionCode = _state.value.pendingPrintTransactionCode
        if (transactionCode.isNullOrBlank()) {
            Log.e(TAG, "No pending print transaction to retry")
            skipPrint()
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = ProcessStatus.Loading,
                    loadingMessage = "Reintentando impresión...",
                    isPrinting = true
                )
            }

            printDocumentsUseCase(transactionCode)
                .onSuccess { printedCount ->
                    Log.d(TAG, "Retry print successful: $printedCount documents")
                    _state.update {
                        it.copy(
                            status = ProcessStatus.Success("La transacción fue cerrada con éxito"),
                            isPrinting = false,
                            pendingPrintTransactionCode = null
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Retry print failed: ${error.message}")
                    _state.update {
                        it.copy(
                            status = ProcessStatus.PrintError(
                                error.message ?: "Error al imprimir los documentos"
                            ),
                            isPrinting = false
                        )
                    }
                }
        }
    }

    fun skipPrint() {
        Log.d(TAG, "Skipping print, showing success")
        _state.update {
            it.copy(
                status = ProcessStatus.Success("La transacción fue cerrada con éxito"),
                isPrinting = false,
                pendingPrintTransactionCode = null
            )
        }
    }

    private fun openTerminal() {
        viewModelScope.launch {
            // Format current date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
            val currentDate = dateFormat.format(Date())

            // Set loading state with message
            _state.update {
                it.copy(
                    status = ProcessStatus.Loading,
                    loadingMessage = "Aperturando el equipo para el dia\n$currentDate"
                )
            }

            openTerminalUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        // Already in loading state
                    }
                    is Resource.Success -> {
                        result.data?.let { openStationResult ->
                            // Save sessionId and stationId to SessionManager
                            sessionManager.saveStationInfo(
                                sessionId = openStationResult.sessionId,
                                stationId = openStationResult.stationId
                            )

                            // Update global station status
                            stationStatus.open()

                            // Determine success message based on whether it was a new session
                            val message = if (openStationResult.isNewSession) {
                                "Dispositivo aperturado"
                            } else {
                                "Dispositivo ya estaba previamente aperturado"
                            }

                            _state.update {
                                it.copy(status = ProcessStatus.Success(message))
                            }
                        } ?: run {
                            _state.update {
                                it.copy(status = ProcessStatus.Error("Respuesta vacía del servidor"))
                            }
                        }
                    }
                    is Resource.Error -> {
                        val errorMessage = "Hubo Un error al Aperturar el dispositivo\nError: ${result.message ?: "Error desconocido"}"
                        _state.update {
                            it.copy(status = ProcessStatus.Error(errorMessage))
                        }
                    }
                }
            }
        }
    }

    private fun closeTerminal() {
        viewModelScope.launch {
            // Format current date
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
            val currentDate = dateFormat.format(Date())

            // Set loading state with message
            _state.update {
                it.copy(
                    status = ProcessStatus.Loading,
                    loadingMessage = "Cerrando el equipo para el dia\n$currentDate"
                )
            }

            closeTerminalUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        // Already in loading state
                    }
                    is Resource.Success -> {
                        result.data?.let { closeStationResult ->
                            if (closeStationResult.success) {
                                // Update global station status
                                stationStatus.close()

                                _state.update {
                                    it.copy(status = ProcessStatus.Success("Terminal cerrada exitosamente"))
                                }
                            } else {
                                _state.update {
                                    it.copy(status = ProcessStatus.Error("Hubo Un error al Cerrar el dispositivo\nError: No se pudo cerrar la terminal"))
                                }
                            }
                        } ?: run {
                            _state.update {
                                it.copy(status = ProcessStatus.Error("Respuesta vacía del servidor"))
                            }
                        }
                    }
                    is Resource.Error -> {
                        val errorMessage = "Hubo Un error al Cerrar el dispositivo\nError: ${result.message ?: "Error desconocido"}"
                        _state.update {
                            it.copy(status = ProcessStatus.Error(errorMessage))
                        }
                    }
                }
            }
        }
    }

    private fun closeDataphone() {
        viewModelScope.launch {
            Log.d(TAG, "closeDataphone() called")

            // Set loading state
            _state.update {
                it.copy(
                    status = ProcessStatus.Loading,
                    loadingMessage = "Cerrando datáfono..."
                )
            }

            try {
                // Step 1: Get point of sale code from session
                val pointOfSaleCode = sessionManager.getStationId().first() ?: run {
                    _state.update {
                        it.copy(status = ProcessStatus.Error("No se encontró el código de punto de venta"))
                    }
                    return@launch
                }

                // Step 2: Call PAX to execute the close
                val dataphoneResultWrapper = dataphoneManager.closeDataphone()

                val dataphoneResult = dataphoneResultWrapper.getOrElse { error ->
                    _state.update {
                        it.copy(status = ProcessStatus.Error(error.message ?: "Error al cerrar el datáfono"))
                    }
                    return@launch
                }

                if (!dataphoneResult.success) {
                    _state.update {
                        it.copy(status = ProcessStatus.Error(dataphoneResult.errorMessage ?: "Error al cerrar el datáfono"))
                    }
                    return@launch
                }

                // Step 3: Build the PaxCloseResponseDto from the result
                val paxResponse = PaxCloseResponseDto(
                    baseAmount = "CRC0.00",
                    cardholder = "",
                    recibo = dataphoneResult.batchNumber ?: "000000",
                    stan = dataphoneResult.batchNumber ?: "",
                    ticket = dataphoneResult.ticket ?: "",
                    totalAmount = dataphoneResult.salesTotal.toString()
                )

                // Get terminal ID from state
                val terminalId = dataphoneState.getTerminalId()

                // Step 4: Send close to megapos API
                paymentRepository.closeDataphone(
                    pointOfSaleCode = pointOfSaleCode,
                    terminalId = terminalId.takeIf { it.isNotBlank() },
                    paxResponse = paxResponse
                ).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            // Already in loading state
                        }
                        is Resource.Success -> {
                            val response = result.data
                            val salesCount = response?.salesCount ?: dataphoneResult.salesCount
                            val salesTotal = response?.salesTotal ?: dataphoneResult.salesTotal

                            // Imprimir comprobante de cierre
                            try {
                                val userName = sessionManager.getUserName().first() ?: "Usuario"
                                val businessUnitName = sessionManager.getBusinessUnitName().first() ?: "Megasuper"
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("es", "CR"))

                                val receiptText = LocalPrintTemplates.buildDataphoneCloseReceipt(
                                    userName = userName,
                                    terminalId = response?.terminalId ?: terminalId,
                                    closeDate = response?.closeDate ?: dateFormat.format(Date()),
                                    salesCount = salesCount,
                                    salesTotal = salesTotal,
                                    reversalsCount = response?.reversalsCount ?: 0,
                                    reversalsTotal = response?.reversalsTotal ?: 0.0,
                                    netTotal = response?.netTotal ?: salesTotal,
                                    voucher = response?.voucher,
                                    businessUnitName = businessUnitName
                                )

                                printerManager.printText(receiptText)
                                Log.d(TAG, "Comprobante de cierre impreso exitosamente")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al imprimir comprobante de cierre", e)
                            }

                            // Format sales total as currency
                            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
                                maximumFractionDigits = 0
                            }
                            val formattedTotal = numberFormat.format(salesTotal)

                            val successMessage = "Cierre completado exitosamente\n\n" +
                                    "Transacciones: $salesCount\n" +
                                    "Total: $formattedTotal"

                            _state.update {
                                it.copy(status = ProcessStatus.Success(successMessage))
                            }
                        }
                        is Resource.Error -> {
                            _state.update {
                                it.copy(status = ProcessStatus.Error(result.message ?: "Error al enviar cierre a megapos"))
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during closeDataphone", e)
                _state.update {
                    it.copy(status = ProcessStatus.Error("Error inesperado: ${e.localizedMessage}"))
                }
            }
        }
    }

    fun onEvent(event: ProcessEvent) {
        when (event) {
            is ProcessEvent.GoBack -> {
                // Handle go back - this will be handled by navigation callback
            }
            is ProcessEvent.RetryProcess -> {
                // Retry the process if needed
            }
        }
    }
}
