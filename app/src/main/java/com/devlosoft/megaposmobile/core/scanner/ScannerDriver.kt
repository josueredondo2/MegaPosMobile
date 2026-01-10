package com.devlosoft.megaposmobile.core.scanner

import android.content.Context
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEvent
import com.devlosoft.megaposmobile.domain.model.ReaderBrand

/**
 * Interface for barcode scanner drivers.
 * Different scanner brands implement this interface to provide
 * brand-specific scanning behavior.
 *
 * Scanners can be:
 * - Keyboard-based (Zebra): Use processKeyEvent()
 * - Broadcast-based (PAX): Use registerBroadcastReceiver()
 */
interface ScannerDriver {
    /**
     * Get the brand of this scanner driver.
     */
    fun getBrand(): ReaderBrand

    /**
     * Whether this driver uses broadcast receiver for scan data.
     * If true, caller should use registerBroadcastReceiver() instead of processKeyEvent().
     */
    fun usesBroadcastReceiver(): Boolean = false

    /**
     * Whether this driver requires persistent focus on the article TextField.
     * PAX uses this mode - characters go directly to TextField, Enter submits.
     */
    fun requiresPersistentFocus(): Boolean = false

    /**
     * Set the FocusRequester for the article TextField.
     * Call this when the screen initializes.
     */
    fun setFocusRequester(focusRequester: FocusRequester) {}

    /**
     * Called when the article TextField loses focus.
     * The driver decides whether to restore focus.
     */
    fun onFocusLost() {}

    /**
     * Request focus on the article TextField.
     * Call this when the TextField is ready to receive focus.
     */
    fun requestFocus() {}

    /**
     * Register broadcast receiver for scan data (for broadcast-based scanners like PAX).
     * @param context Application context
     * @param onBarcodeScanned Callback when barcode is scanned
     */
    fun registerBroadcastReceiver(context: Context, onBarcodeScanned: (String) -> Unit) {}

    /**
     * Unregister broadcast receiver.
     * @param context Application context
     */
    fun unregisterBroadcastReceiver(context: Context) {}

    /**
     * Process a key event and return the completed barcode if detected.
     *
     * @param keyEvent The Compose KeyEvent to process
     * @return The complete barcode string if scan completed, null otherwise
     */
    fun processKeyEvent(keyEvent: KeyEvent): String?

    /**
     * Check if we should consume this key event (prevent it from reaching other handlers).
     * IMPORTANT: Call this AFTER processKeyEvent for correct behavior.
     */
    fun shouldConsumeEvent(keyEvent: KeyEvent): Boolean

    /**
     * Clear the buffer (e.g., on screen exit or timeout).
     */
    fun reset()

    /**
     * Check if buffer is empty.
     */
    fun isEmpty(): Boolean
}
