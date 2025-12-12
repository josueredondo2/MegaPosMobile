package com.devlosoft.megaposmobile.core.printer

import com.devlosoft.megaposmobile.core.printer.drivers.ZebraZQ511Driver
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating printer drivers based on the printer model.
 * To add support for a new printer:
 * 1. Add the model to PrinterModel enum
 * 2. Create a new driver class implementing PrinterDriver
 * 3. Add the case to createDriver()
 */
@Singleton
class PrinterDriverFactory @Inject constructor() {

    /**
     * Creates the appropriate printer driver for the given model
     * @param model The printer model to create a driver for
     * @return PrinterDriver implementation for the model
     */
    fun createDriver(model: PrinterModel): PrinterDriver {
        return when (model) {
            PrinterModel.ZEBRA_ZQ511 -> ZebraZQ511Driver()
            // Add future printer models here:
            // PrinterModel.EPSON_TM_T20 -> EpsonTMT20Driver()
            // PrinterModel.STAR_TSP100 -> StarTSP100Driver()
        }
    }
}
