package com.devlosoft.megaposmobile.presentation.todaytransactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.usecase.PrintDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayTransactionsViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val printDocumentsUseCase: PrintDocumentsUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(TodayTransactionsState())
    val state: StateFlow<TodayTransactionsState> = _state.asStateFlow()

    init {
        loadTransactions()
    }

    fun onEvent(event: TodayTransactionsEvent) {
        when (event) {
            is TodayTransactionsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query, currentPage = 0) }
                applyFilter()
            }
            is TodayTransactionsEvent.SearchTypeChanged -> {
                _state.update { it.copy(searchType = event.type, currentPage = 0) }
                applyFilter()
            }
            is TodayTransactionsEvent.ReprintTransaction -> {
                reprintTransaction(event.transactionId)
            }
            is TodayTransactionsEvent.LoadNextPage -> {
                _state.update { it.copy(currentPage = it.currentPage + 1) }
            }
            is TodayTransactionsEvent.DismissError -> {
                _state.update { it.copy(error = null, printError = null) }
            }
            is TodayTransactionsEvent.DismissPrintSuccess -> {
                _state.update { it.copy(printSuccess = false) }
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            val workstationId = sessionManager.getStationId().first()
            if (workstationId.isNullOrEmpty()) {
                _state.update { it.copy(isLoading = false, error = "No se encontró la estación de trabajo") }
                return@launch
            }

            billingRepository.getTodayCompletedTransactions(workstationId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        val transactions = result.data ?: emptyList()
                        _state.update {
                            it.copy(
                                isLoading = false,
                                transactions = transactions,
                                filteredTransactions = transactions,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        val currentState = _state.value
        val query = currentState.searchQuery.lowercase().trim()

        val filtered = if (query.isEmpty()) {
            currentState.transactions
        } else {
            currentState.transactions.filter { transaction ->
                when (currentState.searchType) {
                    SearchType.BY_ID -> transaction.transactionId.lowercase().contains(query)
                    SearchType.BY_CUSTOMER -> transaction.displayCustomerName.lowercase().contains(query)
                }
            }
        }

        _state.update { it.copy(filteredTransactions = filtered) }
    }

    private fun reprintTransaction(transactionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isPrinting = true, printError = null) }

            val result = printDocumentsUseCase(
                transactionId = transactionId,
                templateId = "01-FC",
                isReprint = true,
                copyNumber = 0
            )

            result.fold(
                onSuccess = { count ->
                    _state.update {
                        it.copy(
                            isPrinting = false,
                            printSuccess = true,
                            printError = null
                        )
                    }
                },
                onFailure = { exception ->
                    _state.update {
                        it.copy(
                            isPrinting = false,
                            printSuccess = false,
                            printError = exception.message ?: "Error al reimprimir"
                        )
                    }
                }
            )
        }
    }
}
