package com.devlosoft.megaposmobile.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 * Using @Serializable annotation as recommended by Navigation Compose 2.8.0+
 */

// Routes without arguments
@Serializable
data object Login

@Serializable
data object Home

@Serializable
data object Configuration

@Serializable
data object AdvancedOptions

@Serializable
data object TransactionDetail

@Serializable
data object TodayTransactions

// Routes with arguments
@Serializable
data class Billing(
    val skipRecoveryCheck: Boolean = false,
    val resetState: Boolean = false
)

@Serializable
data class Process(val processType: String)

@Serializable
data class PaymentProcess(val transactionId: String, val amount: Double)

@Serializable
data class Customer(val identification: String)

// Nested navigation graph marker for sharing ViewModel between Billing and TransactionDetail
@Serializable
data object BillingGraph
