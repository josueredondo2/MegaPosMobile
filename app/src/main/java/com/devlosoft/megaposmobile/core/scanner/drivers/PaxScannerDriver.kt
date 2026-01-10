package com.devlosoft.megaposmobile.core.scanner.drivers

import com.devlosoft.megaposmobile.core.scanner.ScannerDriver
import com.devlosoft.megaposmobile.domain.model.ReaderBrand

/**
 * Scanner driver for PAX A920Pro hardware scanners.
 * TODO: Implement PAX scanner functionality.
 */
class PaxScannerDriver : ScannerDriver {

    override fun getBrand(): ReaderBrand = ReaderBrand.PAX

    override fun processKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): String? = null

    override fun shouldConsumeEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean = false

    override fun reset() {}

    override fun isEmpty(): Boolean = true
}
