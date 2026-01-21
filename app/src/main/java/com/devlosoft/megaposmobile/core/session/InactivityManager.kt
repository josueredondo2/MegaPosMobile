package com.devlosoft.megaposmobile.core.session

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that manages inactivity timeout for automatic logout.
 * After 50 minutes of inactivity (no touch events), emits a logout event.
 */
@Singleton
class InactivityManager @Inject constructor() {

    companion object {
        private const val TAG = "InactivityManager"
        private const val DEFAULT_TIMEOUT_MINUTES = 50
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var backgroundTimestamp: Long? = null
    private var currentTimeoutMs: Long = DEFAULT_TIMEOUT_MINUTES * 60 * 1000L
    private var remainingTimeWhenBackgrounded: Long = currentTimeoutMs

    private val _isActive = MutableStateFlow(false)
    private val _logoutEvent = MutableSharedFlow<Unit>(replay = 0)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    /**
     * Start tracking inactivity. Call after successful login.
     * @param timeoutMinutes The inactivity timeout in minutes (default: 50)
     */
    fun startTracking(timeoutMinutes: Int = DEFAULT_TIMEOUT_MINUTES) {
        Log.d(TAG, "Starting inactivity tracking with timeout: $timeoutMinutes minutes")
        currentTimeoutMs = timeoutMinutes * 60 * 1000L
        _isActive.value = true
        backgroundTimestamp = null
        remainingTimeWhenBackgrounded = currentTimeoutMs
        startTimer(currentTimeoutMs)
    }

    /**
     * Stop tracking inactivity. Call when user logs out manually.
     */
    fun stopTracking() {
        Log.d(TAG, "Stopping inactivity tracking")
        _isActive.value = false
        timerJob?.cancel()
        timerJob = null
        backgroundTimestamp = null
        remainingTimeWhenBackgrounded = currentTimeoutMs
    }

    /**
     * Reset the inactivity timer. Call on each touch event.
     */
    fun resetTimer() {
        if (!_isActive.value) return
        Log.d(TAG, "Resetting inactivity timer")
        timerJob?.cancel()
        startTimer(currentTimeoutMs)
    }

    /**
     * Called when app goes to background (ON_PAUSE).
     * Saves the current timestamp to calculate elapsed time when returning.
     */
    fun onAppBackground() {
        if (!_isActive.value) return
        Log.d(TAG, "App going to background")
        backgroundTimestamp = System.currentTimeMillis()
        // Cancel timer while in background
        timerJob?.cancel()
    }

    /**
     * Called when app returns to foreground (ON_RESUME).
     * Checks if timeout expired while in background.
     */
    fun onAppForeground() {
        if (!_isActive.value) return

        val savedTimestamp = backgroundTimestamp
        if (savedTimestamp == null) {
            Log.d(TAG, "App returning to foreground - no background timestamp")
            // App wasn't in background, just continue with current timer
            return
        }

        val elapsedInBackground = System.currentTimeMillis() - savedTimestamp
        Log.d(TAG, "App returning to foreground - elapsed in background: ${elapsedInBackground}ms")

        backgroundTimestamp = null

        if (elapsedInBackground >= currentTimeoutMs) {
            // Timeout expired while in background
            Log.d(TAG, "Inactivity timeout expired while in background - triggering logout")
            triggerLogout()
        } else {
            // Resume timer with remaining time minus background time
            val remainingTime = currentTimeoutMs - elapsedInBackground
            Log.d(TAG, "Resuming timer with remaining time: ${remainingTime}ms")
            startTimer(remainingTime)
        }
    }

    private fun startTimer(durationMs: Long) {
        timerJob?.cancel()
        timerJob = scope.launch {
            Log.d(TAG, "Timer started for ${durationMs}ms")
            delay(durationMs)
            if (_isActive.value) {
                Log.d(TAG, "Inactivity timeout expired - triggering logout")
                triggerLogout()
            }
        }
    }

    private fun triggerLogout() {
        scope.launch {
            _logoutEvent.emit(Unit)
        }
    }
}
