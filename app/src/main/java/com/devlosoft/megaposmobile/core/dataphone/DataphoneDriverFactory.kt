package com.devlosoft.megaposmobile.core.dataphone

import com.devlosoft.megaposmobile.core.dataphone.drivers.PaxBacDriver
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory para crear drivers de datáfono según el proveedor.
 */
@Singleton
class DataphoneDriverFactory @Inject constructor() {

    /**
     * Crea un driver de datáfono para el proveedor especificado.
     * @param provider Proveedor de datáfono
     * @return Driver correspondiente al proveedor
     */
    fun createDriver(provider: DatafonoProvider): DataphoneDriver {
        return when (provider) {
            DatafonoProvider.PAX_BAC -> PaxBacDriver()
            // Futuro: DatafonoProvider.INGENICO -> IngenicoDriver()
            // Futuro: DatafonoProvider.VERIFONE -> VerifoneDriver()
        }
    }
}
