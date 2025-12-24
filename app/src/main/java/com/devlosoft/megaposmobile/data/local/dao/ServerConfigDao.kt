package com.devlosoft.megaposmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devlosoft.megaposmobile.data.local.entity.ServerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServerConfig(config: ServerConfigEntity)

    @Query("SELECT * FROM server_config WHERE isActive = 1 LIMIT 1")
    fun getActiveServerConfig(): Flow<ServerConfigEntity?>

    @Query("SELECT * FROM server_config WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServerConfigSync(): ServerConfigEntity?

    @Query("SELECT * FROM server_config")
    fun getAllServerConfigs(): Flow<List<ServerConfigEntity>>

    @Query("DELETE FROM server_config WHERE id = :id")
    suspend fun deleteServerConfig(id: Int)

    @Query("UPDATE server_config SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Int, timestamp: Long)

    @Query("UPDATE server_config SET dataphoneTerminalId = :terminalId WHERE id = 1")
    suspend fun updateDataphoneTerminalId(terminalId: String)

    @Query("SELECT dataphoneTerminalId FROM server_config WHERE id = 1")
    suspend fun getDataphoneTerminalId(): String?
}
