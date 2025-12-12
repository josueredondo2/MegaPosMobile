package com.devlosoft.megaposmobile.presentation.process

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.usecase.CloseTerminalUseCase
import com.devlosoft.megaposmobile.domain.usecase.OpenTerminalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    const val FINALIZE_PAYMENT = "finalizePayment"
}

@HiltViewModel
class ProcessViewModel @Inject constructor(
    private val openTerminalUseCase: OpenTerminalUseCase,
    private val closeTerminalUseCase: CloseTerminalUseCase,
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager,
    private val stationStatus: StationStatus,
    private val serverConfigDao: ServerConfigDao,
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
                    loadingMessage = "Favor realice el pago en el datafono\npor el monto de $formattedAmount"
                )
            }

            // Simulate 3 second wait for payment
            delay(3000)

            // Get session data
            val sessionId = sessionManager.getSessionId().first()
            val stationId = sessionManager.getStationId().first()

            if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                _state.update {
                    it.copy(status = ProcessStatus.Error("No hay sesión activa"))
                }
                return@launch
            }

            // Call finalize transaction
            Log.d(TAG, "Calling finalizeTransaction...")
            billingRepository.finalizeTransaction(
                sessionId = sessionId,
                workstationId = stationId,
                transactionId = transactionId
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
        }
    }

    private suspend fun fetchAndPrintDocuments(transactionId: String) {
        Log.d(TAG, "fetchAndPrintDocuments() called for transaction: $transactionId")

        billingRepository.getPrintDocuments(
            transactionId = transactionId,
            templateId = "01-FC",
            isReprint = false,
            copyNumber = 0
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    Log.d(TAG, "Fetching print documents...")
                }
                is Resource.Success -> {
                    val documents = result.data ?: emptyList()
                    Log.d(TAG, "Received ${documents.size} print documents")

                    // Print each document
                    for (document in documents) {
                        printDocument(document)
                    }

                    // Update state after printing
                    _state.update {
                        it.copy(status = ProcessStatus.Success("La transacción fue cerrada con éxito"))
                    }
                    Log.d(TAG, "State updated successfully after printing")
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error fetching print documents: ${result.message}")
                    // Still show success for the transaction, just log the print error
                    _state.update {
                        it.copy(status = ProcessStatus.Success("La transacción fue cerrada con éxito"))
                    }
                }
            }
        }
    }

    private suspend fun printDocument(document: PrintDocument) {
        Log.d(TAG, "Printing document: ${document.documentType}")

        try {
            val config = serverConfigDao.getActiveServerConfigSync()

            if (config == null) {
                Log.e(TAG, "No server config found")
                return
            }

            val printText = document.printText
            Log.d(TAG, "Print text length: ${printText.length}")

            val printerModel = PrinterModel.fromString(config.printerModel)
            Log.d(TAG, "Using printer model: ${printerModel.displayName}")

            // Print using PrinterManager (handles both IP and Bluetooth internally)
            val result = printerManager.printText(printText)

            result.fold(
                onSuccess = { message ->
                    Log.d(TAG, "Print success: $message")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Print error: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception while printing: ${e.message}", e)
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
