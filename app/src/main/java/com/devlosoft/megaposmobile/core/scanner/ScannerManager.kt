package com.devlosoft.megaposmobile.core.scanner

import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.model.ReaderBrand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for scanner operations.
 * Reads configuration from database and provides the appropriate scanner driver.
 */
@Singleton
class ScannerManager @Inject constructor(
    private val serverConfigDao: ServerConfigDao,
    private val scannerDriverFactory: ScannerDriverFactory
) {
    private var currentDriver: ScannerDriver? = null
    private var currentBrand: ReaderBrand? = null

    /**
     * Get the scanner driver based on current configuration.
     * Creates a new driver if the brand has changed.
     *
     * @return The configured ScannerDriver implementation
     */
    suspend fun getDriver(): ScannerDriver {
        val config = serverConfigDao.getActiveServerConfigSync()
        val brand = ReaderBrand.fromString(config?.readerBrand ?: "ZEBRA")

        // Create new driver if brand changed or no driver exists
        if (currentDriver == null || currentBrand != brand) {
            currentDriver?.reset() // Clean up previous driver
            currentDriver = scannerDriverFactory.createDriver(brand)
            currentBrand = brand
        }

        return currentDriver!!
    }

    /**
     * Get the current driver synchronously (returns cached driver or creates default).
     * Use this when you need a driver immediately without suspend.
     */
    fun getDriverSync(): ScannerDriver {
        return currentDriver ?: scannerDriverFactory.createDriver(ReaderBrand.ZEBRA).also {
            currentDriver = it
            currentBrand = ReaderBrand.ZEBRA
        }
    }

    /**
     * Reset the current driver's state.
     */
    fun resetDriver() {
        currentDriver?.reset()
    }
}
