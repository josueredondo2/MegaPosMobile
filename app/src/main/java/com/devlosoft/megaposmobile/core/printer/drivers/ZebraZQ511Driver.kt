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
        val charWidth = 10       // Font C character width in dots
        val lineSpacing = 22     // Line spacing for Font C
        val charsPerLine = printWidth / charWidth  // ~57 chars fit per line

        // Remove trailing empty lines, then add controlled spacing
        val trimmedLines = lines.dropLastWhile { it.isBlank() }
        val extraLinesAtStart = 5   // Extra blank lines at start
        val extraLinesAtEnd = 10    // Extra blank lines for easy tear-off

        // Calculate total physical lines accounting for word-wrap on long lines
        val totalPhysicalLines = trimmedLines.sumOf { line ->
            if (line.isEmpty()) 1
            else maxOf(1, (line.length + charsPerLine - 1) / charsPerLine)
        }

        val commands = StringBuilder()

        // Start label
        commands.append("^XA\n")

        // Encoding and dimensions
        commands.append("^CI28\n")  // UTF-8 for special characters (ñ, á, etc.)
        commands.append("^PW$printWidth\n")
        commands.append("^LL${10 + ((totalPhysicalLines + extraLinesAtStart + extraLinesAtEnd) * lineSpacing)}\n")

        // Print quality settings
        commands.append("~SD12\n")   // Darkness: 12 (lighter, normal receipt color)
        commands.append("^PR6,6,6\n") // Speed: 6 (faster printing)

        // Print each line with ^FB (Field Block) for automatic word-wrap
        var yPosition = 10 + (extraLinesAtStart * lineSpacing)  // Start after blank lines

        trimmedLines.forEach { line ->
            val printLine = if (line.isEmpty()) " " else line

            // Calculate how many physical lines this text needs when wrapped
            val physicalLines = if (printLine.isBlank()) 1
                               else maxOf(1, (printLine.length + charsPerLine - 1) / charsPerLine)

            // ^ACN: Font C (medium bitmap 18x10)
            // ^FB: Field Block - enables automatic word-wrap
            //   printWidth = block width in dots
            //   physicalLines = max lines allowed for wrapping
            //   0 = no extra line spacing
            //   L = left-aligned
            //   0 = no hanging indent
            commands.append("^FO0,$yPosition")
            commands.append("^ACN,$fontMultiplier,$fontMultiplier")
            commands.append("^FB$printWidth,$physicalLines,0,L,0")
            commands.append("^FD$printLine^FS\n")

            yPosition += physicalLines * lineSpacing
        }

        commands.append("^XZ\n")  // End label
        return commands.toString()
    }
}
