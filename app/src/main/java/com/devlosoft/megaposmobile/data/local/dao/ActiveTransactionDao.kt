package com.devlosoft.megaposmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devlosoft.megaposmobile.data.local.entity.ActiveTransactionEntity

/**
 * DAO for managing the active transaction record.
 * Used to persist and retrieve the current transaction ID for recovery purposes.
 */
@Dao
interface ActiveTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveTransaction(transaction: ActiveTransactionEntity)

    @Query("SELECT * FROM active_transaction WHERE id = 1")
    suspend fun getActiveTransaction(): ActiveTransactionEntity?

    @Query("DELETE FROM active_transaction WHERE id = 1")
    suspend fun clearActiveTransaction()
}
