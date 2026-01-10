package com.devlosoft.megaposmobile.core.scanner

import com.devlosoft.megaposmobile.core.scanner.drivers.PaxScannerDriver
import com.devlosoft.megaposmobile.core.scanner.drivers.ZebraScannerDriver
import com.devlosoft.megaposmobile.domain.model.ReaderBrand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating scanner drivers based on the configured reader brand.
 */
@Singleton
class ScannerDriverFactory @Inject constructor() {

    /**
     * Create a scanner driver for the specified brand.
     *
     * @param brand The reader brand to create a driver for
     * @return A ScannerDriver implementation for the specified brand
     */
    fun createDriver(brand: ReaderBrand): ScannerDriver {
        return when (brand) {
            ReaderBrand.ZEBRA -> ZebraScannerDriver()
            ReaderBrand.PAX -> PaxScannerDriver()
        }
    }
}
