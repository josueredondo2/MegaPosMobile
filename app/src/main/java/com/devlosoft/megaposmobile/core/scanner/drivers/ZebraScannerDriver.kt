package com.devlosoft.megaposmobile.core.scanner.drivers

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import com.devlosoft.megaposmobile.core.scanner.ScannerDriver
import com.devlosoft.megaposmobile.domain.model.ReaderBrand

/**
 * Scanner driver for Zebra hardware scanners.
 *
 * Scanners configured in "Keyboard Wedge" mode send characters as key events.
 * This driver accumulates rapid keystrokes and detects barcode completion on ENTER.
 *
 * Supported devices:
 * - Zebra handhelds (DataWedge in Keystroke Output mode)
 */
class ZebraScannerDriver : ScannerDriver {

    companion object {
        private const val MAX_KEY_INTERVAL_MS = 100L // Max time between scanner keystrokes
        private const val MIN_BARCODE_LENGTH = 4     // Minimum valid barcode length
        private const val SCAN_COMPLETE_TIMEOUT_MS = 150L // Time to wait after last digit
    }

    private val buffer = StringBuilder()
    private var lastKeyTime = 0L
    private var lastEventWasConsumed = false

    override fun getBrand(): ReaderBrand = ReaderBrand.ZEBRA

    override fun processKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): String? {
        val currentTime = System.currentTimeMillis()
        val nativeKeyCode = keyEvent.key.nativeKeyCode
        val isKeyDown = keyEvent.type == KeyEventType.KeyDown
        val isKeyUp = keyEvent.type == KeyEventType.KeyUp

        // For KEY_UP events on non-digit keys, check if we have a complete barcode
        // This handles scanner trigger release (keycode 103) which comes after all digits
        if (isKeyUp && nativeKeyCode !in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            val timeSinceLastKey = currentTime - lastKeyTime
            if (buffer.length >= MIN_BARCODE_LENGTH && timeSinceLastKey <= SCAN_COMPLETE_TIMEOUT_MS) {
                val barcode = getAndClearBuffer()
                lastEventWasConsumed = true
                return barcode
            }
            lastEventWasConsumed = false
            return null
        }

        // Only process KEY_DOWN events for the rest
        if (!isKeyDown) {
            lastEventWasConsumed = false
            return null
        }

        return when {
            // ENTER = end of barcode scan
            keyEvent.key == Key.Enter -> {
                val barcode = getAndClearBuffer()
                if (barcode.length >= MIN_BARCODE_LENGTH) {
                    lastEventWasConsumed = true
                    barcode
                } else {
                    lastEventWasConsumed = false
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

    override fun shouldConsumeEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        return lastEventWasConsumed
    }

    private fun getAndClearBuffer(): String {
        val result = buffer.toString()
        buffer.clear()
        lastKeyTime = 0L
        return result
    }

    override fun reset() {
        buffer.clear()
        lastKeyTime = 0L
    }

    override fun isEmpty(): Boolean = buffer.isEmpty()
}
