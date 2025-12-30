package com.devlosoft.megaposmobile.presentation.todaytransactions

sealed class TodayTransactionsEvent {
    data class SearchQueryChanged(val query: String) : TodayTransactionsEvent()
    data class SearchTypeChanged(val type: SearchType) : TodayTransactionsEvent()
    data class ReprintTransaction(val transactionId: String) : TodayTransactionsEvent()
    data object LoadNextPage : TodayTransactionsEvent()
    data object DismissError : TodayTransactionsEvent()
    data object DismissPrintSuccess : TodayTransactionsEvent()
}
