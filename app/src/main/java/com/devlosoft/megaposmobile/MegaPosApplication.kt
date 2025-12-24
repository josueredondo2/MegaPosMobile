package com.devlosoft.megaposmobile

import android.app.Application
import android.util.Log
import com.devlosoft.megaposmobile.core.state.DataphoneState
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MegaPosApplication : Application() {

    companion object {
        private const val TAG = "MegaPosApplication"
    }

    @Inject
    lateinit var dataphoneState: DataphoneState

    @Inject
    lateinit var serverConfigDao: ServerConfigDao

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeDataphoneState()
    }

    private fun initializeDataphoneState() {
        applicationScope.launch {
            try {
                val storedTerminalId = serverConfigDao.getDataphoneTerminalId() ?: ""
                if (storedTerminalId.isNotBlank()) {
                    dataphoneState.setTerminalId(storedTerminalId)
                    Log.d(TAG, "Loaded terminal ID from DB: $storedTerminalId")
                } else {
                    Log.d(TAG, "No terminal ID stored in DB")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading terminal ID from DB", e)
            }
        }
    }
}
