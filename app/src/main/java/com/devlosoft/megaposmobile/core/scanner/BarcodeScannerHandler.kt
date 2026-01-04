package com.devlosoft.megaposmobile.core.scanner

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles barcode scanner input from Zebra hardware scanners.
 *
 * Scanners configured in "Keyboard Wedge" mode send characters as key events.
 * This handler accumulates rapid keystrokes and detects barcode completion on ENTER.
 *
 * Supported devices:
 * - Zebra handhelds (DataWedge in Keystroke Output mode)
 */
@Singleton
class BarcodeScannerHandler @Inject constructor() {

    companion object {
        private const val MAX_KEY_INTERVAL_MS = 100L // Max time between scanner keystrokes
        private const val MIN_BARCODE_LENGTH = 4     // Minimum valid barcode length
    }

    private val buffer = StringBuilder()
    private var lastKeyTime = 0L
    private var lastEventWasConsumed = false // Track if last processKeyEvent consumed an ENTER

    /**
     * Process a key event and return the completed barcode if detected.
     * Uses timing to distinguish between scanner (fast) and manual input (slow).
     * Scanner always has priority.
     *
     * @param keyEvent The Compose KeyEvent to process
     * @return The complete barcode string if ENTER was pressed, null otherwise
     */
    fun processKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): String? {
        // Only process KEY_DOWN events
        if (keyEvent.type != KeyEventType.KeyDown) {
            lastEventWasConsumed = false
            return null
        }

        val currentTime = System.currentTimeMillis()
        val nativeKeyCode = keyEvent.key.nativeKeyCode

        return when {
            // ENTER = end of barcode scan
            keyEvent.key == Key.Enter -> {
                val barcode = getAndClearBuffer()
                // Only consume and return if we have a valid barcode (fast input pattern)
                if (barcode.length >= MIN_BARCODE_LENGTH) {
                    lastEventWasConsumed = true
                    barcode
                } else {
                    lastEventWasConsumed = false // Let ENTER pass to TextField for manual input
                    null
                }
            }

            // Numeric keys (0-9) - typical barcode characters
            nativeKeyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                val timeSinceLastKey = currentTime - lastKeyTime
                val isFirstChar = lastKeyTime == 0L
                val isFastInput = !isFirstChar && timeSinceLastKey <= MAX_KEY_INTERVAL_MS

                // If slow input (manual typing), clear buffer and don't consume
                if (!isFirstChar && timeSinceLastKey > MAX_KEY_INTERVAL_MS) {
                    buffer.clear()
                }

                // Add digit to buffer
                val digit = (nativeKeyCode - KeyEvent.KEYCODE_0).toString()
                buffer.append(digit)
                lastKeyTime = currentTime

                // Only consume if it's fast input (scanner pattern)
                // First char and slow chars pass through to TextField
                lastEventWasConsumed = isFastInput
                null
            }

            else -> {
                lastEventWasConsumed = false
                null
            }
        }
    }

    /**
     * Check if we should consume this key event (prevent it from reaching other handlers).
     * IMPORTANT: Call this AFTER processKeyEvent for correct behavior.
     */
    fun shouldConsumeEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        return lastEventWasConsumed
    }

    /**
     * Get current buffer content and clear it.
     */
    private fun getAndClearBuffer(): String {
        val result = buffer.toString()
        buffer.clear()
        lastKeyTime = 0L
        return result
    }

    /**
     * Clear the buffer (e.g., on screen exit or timeout).
     */
    fun reset() {
        buffer.clear()
        lastKeyTime = 0L
    }

    /**
     * Check if buffer is empty.
     */
    fun isEmpty(): Boolean = buffer.isEmpty()
}
