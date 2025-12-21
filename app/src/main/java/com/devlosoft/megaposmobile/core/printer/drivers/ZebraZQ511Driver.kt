package com.devlosoft.megaposmobile.core.printer.drivers

import com.devlosoft.megaposmobile.core.printer.PrinterDriver
import com.devlosoft.megaposmobile.domain.model.PrinterModel

/**
 * Printer driver for Zebra ZQ511
 * Uses ZPL (Zebra Programming Language) commands
 *
 * Specifications:
 * - 3" (72mm) thermal portable printer
 * - 203 DPI resolution
 * - Receipt style output (~42-48 characters per line)
 * - Monospace font style (Courier 11pt equivalent)
 * - UTF-8 encoding support
 */
class ZebraZQ511Driver : PrinterDriver {

    override fun getModel(): PrinterModel = PrinterModel.ZEBRA_ZQ511

    override fun buildLabel(text: String): ByteArray {
        return buildZPLLabel(text).toByteArray(Charsets.UTF_8)
    }

    override fun buildTestLabel(text: String): ByteArray {
        return buildZPLLabel(text).toByteArray(Charsets.UTF_8)
    }

    /**
     * Generates ZPL commands for printing a POS receipt
     * Optimized for Zebra ZQ511 (3" portable thermal printer - 72mm)
     *
     * Optimal POS receipt configuration:
     * - Paper width: 576 dots (72mm at 203 DPI)
     * - 48 characters per line (standard POS width)
     * - Clear, readable monospace-style font
     * - Pre-formatted text with spaces for alignment
     */
    private fun buildZPLLabel(text: String): String {
        val lines = text.split("\n")

        // === ZEBRA ZQ511 SETTINGS (72mm paper) ===
        // Using Font C (medium fixed-pitch bitmap)
        // Font C base size: 18x10 dots
        val printWidth = 576     // 72mm paper at 203 DPI
        val fontMultiplier = 1   // Multiplier for Font C (1x = 18x10 dots)
        val lineSpacing = 22     // Line spacing for Font C

        // Remove trailing empty lines, then add controlled spacing
        val trimmedLines = lines.dropLastWhile { it.isBlank() }
        val extraLinesAtStart = 5   // Extra blank lines at start
        val extraLinesAtEnd = 10    // Extra blank lines for easy tear-off

        val commands = StringBuilder()

        // Start label
        commands.append("^XA\n")

        // Encoding and dimensions
        commands.append("^CI28\n")  // UTF-8 for special characters (ñ, á, etc.)
        commands.append("^PW$printWidth\n")
        commands.append("^LL${10 + ((trimmedLines.size + extraLinesAtStart + extraLinesAtEnd) * lineSpacing)}\n")

        // Print quality settings
        commands.append("~SD12\n")   // Darkness: 12 (lighter, normal receipt color)
        commands.append("^PR6,6,6\n") // Speed: 6 (faster printing)

        // Print each line preserving pre-formatted spacing
        var yPosition = 10 + (extraLinesAtStart * lineSpacing)  // Start after blank lines

        trimmedLines.forEach { line ->
            val printLine = if (line.isEmpty()) " " else line
            // Use ^ACN (Font C - medium bitmap 18x10) for balanced size and alignment
            commands.append("^FO0,$yPosition^ACN,$fontMultiplier,$fontMultiplier^FD$printLine^FS\n")
            yPosition += lineSpacing
        }

        commands.append("^XZ\n")  // End label
        return commands.toString()
    }
}
