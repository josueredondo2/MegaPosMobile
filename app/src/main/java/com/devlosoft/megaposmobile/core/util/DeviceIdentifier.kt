package com.devlosoft.megaposmobile.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Returns the Android ID which is a unique 64-bit hex string.
     * It's unique per app-signing key, device, and user.
     * This is the recommended approach for device identification on Android 10+
     * since MAC address randomization makes MAC unreliable.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

        Log.d("DeviceIdentifier", "Android ID: $androidId")

        return androidId
    }
}
