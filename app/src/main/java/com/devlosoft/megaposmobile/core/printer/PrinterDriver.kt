package com.devlosoft.megaposmobile.core.printer

import com.devlosoft.megaposmobile.domain.model.PrinterModel

/**
 * Interface for printer drivers.
 * Each printer model should have its own implementation.
 */
interface PrinterDriver {
    /**
     * Returns the printer model this driver supports
     */
    fun getModel(): PrinterModel

    /**
     * Builds the print commands for a label/receipt
     * @param text The text content to print
     * @return ByteArray with the printer-specific commands
     */
    fun buildLabel(text: String): ByteArray

    /**
     * Builds the print commands for a test label
     * @param text The test text to print
     * @return ByteArray with the printer-specific commands
     */
    fun buildTestLabel(text: String): ByteArray
}
