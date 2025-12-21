package com.devlosoft.megaposmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to persist the active transaction ID locally.
 * This allows recovering a transaction if the app is closed unexpectedly.
 * Only one active transaction can exist at a time (id = 1).
 */
@Entity(tableName = "active_transaction")
data class ActiveTransactionEntity(
    @PrimaryKey
    val id: Int = 1,
    val transactionId: String,
    val createdAt: Long = System.currentTimeMillis()
)
