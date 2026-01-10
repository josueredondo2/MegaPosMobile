package com.devlosoft.megaposmobile.core.scanner.drivers

import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEvent
import com.devlosoft.megaposmobile.core.scanner.ScannerDriver
import com.devlosoft.megaposmobile.domain.model.ReaderBrand

/**
 * Scanner driver for PAX A920Pro hardware scanners.
 *
 * PAX scanners operate differently from Zebra:
 * - Characters flow directly to the focused TextField (no interception)
 * - The UI maintains persistent focus on the article TextField
 * - Enter key is handled by TextField's keyboardActions.onDone
 *
 * This is a "pass-through" driver - it doesn't buffer characters.
 * Focus management is handled via setFocusRequester/onFocusLost/requestFocus.
 */
class PaxScannerDriver : ScannerDriver {

    companion object {
        private const val TAG = "PaxScannerDriver"
    }

    private var focusRequester: FocusRequester? = null

    override fun getBrand(): ReaderBrand = ReaderBrand.PAX

    /**
     * PAX requires persistent focus on article TextField.
     * All key events flow directly to the TextField.
     */
    override fun requiresPersistentFocus(): Boolean {
        Log.d(TAG, "requiresPersistentFocus() called, returning true")
        return true
    }

    /**
     * Set the FocusRequester for the article TextField.
     */
    override fun setFocusRequester(focusRequester: FocusRequester) {
        Log.d(TAG, "setFocusRequester() called, focusRequester=$focusRequester")
        this.focusRequester = focusRequester
    }

    /**
     * Called when the article TextField loses focus.
     * PAX always restores focus to keep scanning ready.
     */
    override fun onFocusLost() {
        Log.d(TAG, "onFocusLost() called, will request focus")
        requestFocus()
    }

    /**
     * Request focus on the article TextField.
     */
    override fun requestFocus() {
        Log.d(TAG, "requestFocus() called, focusRequester=$focusRequester")
        try {
            focusRequester?.requestFocus()
            Log.d(TAG, "requestFocus() - focus requested successfully")
        } catch (e: Exception) {
            Log.e(TAG, "requestFocus() - failed: ${e.message}")
        }
    }

    /**
     * Do not intercept key events - let them flow to TextField.
     * Enter is handled by TextField's keyboardActions.onDone.
     */
    override fun processKeyEvent(keyEvent: KeyEvent): String? = null

    /**
     * Do not consume any events - let all pass through to TextField.
     */
    override fun shouldConsumeEvent(keyEvent: KeyEvent): Boolean = false

    override fun reset() {
        focusRequester = null
    }

    override fun isEmpty(): Boolean = true
}
