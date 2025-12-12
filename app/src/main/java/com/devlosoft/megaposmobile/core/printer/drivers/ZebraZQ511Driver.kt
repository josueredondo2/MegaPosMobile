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
     * Generates ZPL commands for printing a label/receipt
     * Optimized for Zebra ZQ511 (3" portable thermal printer - 72mm)
     * Configuration: 72mm width, sales receipt style
     * Compact font for invoices: ~42-48 characters per line
     */
    private fun buildZPLLabel(text: String): String {
        // Split text into lines
        val lines = text.split("\n")

        // Build ZPL commands for receipt style
        // Exact configuration to match virtual printer:
        // - 48 characters per line (72mm)
        // - Monospace font like Courier 11pt
        // - Line height 14pt
        val commands = StringBuilder()
        commands.append("^XA\n")  // Start format
        commands.append("^CI28\n")  // UTF-8 encoding for accents and special characters
        commands.append("^PW576\n")  // Print width (576 dots for 72mm at 203 DPI)
        commands.append("^LL${80 + (lines.size * 28)}\n")  // Dynamic length based on lines
        commands.append("^CF0,24\n")  // Default font: height 24 (100% base size)

        // Add each line with explicit 100% font size
        var yPosition = 15
        lines.forEach { line ->
            if (line.isNotBlank()) {
                commands.append("^FO3,$yPosition^A0N,24,12^FD$line^FS\n")  // 100%: height 24, width 12
                yPosition += 28  // Line spacing for 100%
            }
        }

        commands.append("^XZ")  // End format

        return commands.toString()
    }
}
