package com.devlosoft.megaposmobile.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager,
    private val serverConfigDao: ServerConfigDao
) : ViewModel() {

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    fun onEvent(event: BillingEvent) {
        when (event) {
            is BillingEvent.CustomerSearchQueryChanged -> {
                _state.update { it.copy(customerSearchQuery = event.query) }
            }
            is BillingEvent.SearchCustomer -> {
                searchCustomer()
            }
            is BillingEvent.SelectCustomer -> {
                _state.update { it.copy(selectedCustomer = event.customer) }
            }
            is BillingEvent.ClearCustomerSearch -> {
                _state.update {
                    it.copy(
                        customerSearchQuery = "",
                        customers = emptyList(),
                        selectedCustomer = null
                    )
                }
            }
            is BillingEvent.DismissCustomerSearchError -> {
                _state.update { it.copy(customerSearchError = null) }
            }
            is BillingEvent.StartTransaction -> {
                startTransaction()
            }
            is BillingEvent.DismissCreateTransactionError -> {
                _state.update { it.copy(createTransactionError = null) }
            }
            is BillingEvent.NavigationHandled -> {
                _state.update { it.copy(shouldNavigateToTransaction = false) }
            }
            is BillingEvent.ArticleSearchQueryChanged -> {
                _state.update { it.copy(articleSearchQuery = event.query) }
            }
            is BillingEvent.AddArticle -> {
                addArticle()
            }
            is BillingEvent.DismissAddArticleError -> {
                _state.update { it.copy(addArticleError = null) }
            }
            is BillingEvent.GoBack -> {
                // Handle in screen
            }
        }
    }

    private fun searchCustomer() {
        val query = _state.value.customerSearchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            billingRepository.searchCustomer(query).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isSearchingCustomer = true,
                                customerSearchError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val customers = result.data ?: emptyList()
                        val selectedCustomer = if (customers.size == 1) customers.first() else null

                        _state.update {
                            it.copy(
                                isSearchingCustomer = false,
                                customers = customers,
                                selectedCustomer = selectedCustomer
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isSearchingCustomer = false,
                                customers = emptyList(),
                                selectedCustomer = null,
                                customerSearchError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startTransaction() {
        // Use default customer if none selected
        val customerToUse = _state.value.selectedCustomer ?: Customer.DEFAULT

        // Just update state and navigate - transaction will be created when adding first article
        _state.update {
            it.copy(
                selectedCustomer = customerToUse,
                shouldNavigateToTransaction = true
            )
        }
    }

    private fun addArticle() {
        val articleId = _state.value.articleSearchQuery.trim()
        if (articleId.isBlank()) return

        val transactionCode = _state.value.transactionCode
        if (transactionCode.isBlank()) {
            // Need to create transaction first, then add article
            createTransactionAndAddArticle(articleId)
            return
        }

        // Transaction already exists, just add the article
        addArticleToTransaction(transactionCode, articleId)
    }

    private fun createTransactionAndAddArticle(articleId: String) {
        viewModelScope.launch {
            val sessionId = sessionManager.getSessionId().first()
            val stationId = sessionManager.getStationId().first()

            if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                _state.update {
                    it.copy(addArticleError = "No hay sesión activa")
                }
                return@launch
            }

            // Use default customer if none selected
            val customerToUse = _state.value.selectedCustomer ?: Customer.DEFAULT

            // First create the transaction
            billingRepository.createTransaction(
                sessionId = sessionId,
                workstationId = stationId,
                customerId = customerToUse.partyId.toString(),
                customerIdType = customerToUse.identificationType,
                customerName = customerToUse.name
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = true,
                                addArticleError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val transactionCode = result.data ?: ""
                        _state.update {
                            it.copy(
                                transactionCode = transactionCode,
                                isTransactionCreated = true
                            )
                        }
                        // Now add the article
                        addArticleToTransaction(transactionCode, articleId)
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                addArticleError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun addArticleToTransaction(transactionCode: String, articleId: String) {
        viewModelScope.launch {
            val selectedCustomer = _state.value.selectedCustomer

            billingRepository.addMaterial(
                transactionId = transactionCode,
                itemPosId = articleId,
                quantity = 1.0,
                partyAffiliationTypeCode = selectedCustomer?.affiliateType
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = true,
                                addArticleError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                invoiceData = result.data ?: it.invoiceData,
                                articleSearchQuery = "" // Clear the input
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                addArticleError = result.message
                            )
                        }
                    }
                }
            }
        }
    }
}
