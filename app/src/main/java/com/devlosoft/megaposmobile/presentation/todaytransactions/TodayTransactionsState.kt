package com.devlosoft.megaposmobile.presentation.todaytransactions

import com.devlosoft.megaposmobile.domain.model.TodayTransaction

data class TodayTransactionsState(
    val transactions: List<TodayTransaction> = emptyList(),
    val filteredTransactions: List<TodayTransaction> = emptyList(),
    val searchQuery: String = "",
    val searchType: SearchType = SearchType.BY_ID,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val pageSize: Int = 25,
    val isPrinting: Boolean = false,
    val printSuccess: Boolean = false,
    val printError: String? = null
) {
    val totalCount: Int
        get() = filteredTransactions.size

    val paginatedTransactions: List<TodayTransaction>
        get() {
            val endIndex = minOf((currentPage + 1) * pageSize, filteredTransactions.size)
            return filteredTransactions.take(endIndex)
        }

    val hasMorePages: Boolean
        get() = paginatedTransactions.size < filteredTransactions.size
}

enum class SearchType {
    BY_ID,
    BY_CUSTOMER
}
