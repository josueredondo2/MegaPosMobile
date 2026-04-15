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

    /**
     * Sends a raw command string directly to the printer (no driver formatting)
     * Used for SGD commands like ! U1 setvar
     * @param command Raw command string to send
     * @return Result with success or error message
     */
    suspend fun sendRawCommand(command: String): Result<String>

    /**
     * Sends a command and reads the printer's response
     * Used for SGD getvar queries like ! U1 getvar "device.languages"
     * @param command The query command to send
     * @return Result with the printer's response string
     */
    suspend fun queryPrinter(command: String): Result<String>
}
