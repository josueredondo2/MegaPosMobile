package com.devlosoft.megaposmobile.core.printer

import com.devlosoft.megaposmobile.domain.model.PrinterModel

/**
 * Interface that defines the contract for printer services
 */
interface PrinterService {
    /**
     * Tests the connection to verify the printer is active and responding
     * @return Result with success message or error
     */
    suspend fun testConnection(): Result<String>

    /**
     * Prints text using the configured printer
     * @param text Text to print
     * @param printerModel The printer model to use for formatting
     * @return Result with success or error message
     */
    suspend fun printText(text: String, printerModel: PrinterModel): Result<String>
}
