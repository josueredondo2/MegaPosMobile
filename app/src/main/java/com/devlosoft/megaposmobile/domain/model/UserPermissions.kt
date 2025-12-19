package com.devlosoft.megaposmobile.domain.model

data class UserPermissions(
    val groupCode: String = "",
    val groupName: String = "",
    val processes: Map<String, ProcessPermission> = emptyMap(),
    val screensAccess: List<ScreenAccess> = emptyList()
) {
    /**
     * Check if user has access to a specific process
     */
    fun hasAccess(processKey: String): Boolean {
        return processes[processKey]?.access ?: false
    }

    /**
     * Check if a specific process should be shown
     */
    fun shouldShow(processKey: String): Boolean {
        return processes[processKey]?.show ?: false
    }

    /**
     * Check if user can access and see a specific process
     */
    fun canUse(processKey: String): Boolean {
        val permission = processes[processKey]
        return permission?.access == true && permission.show == true
    }

    companion object {
        // Process keys for mobile POS
        const val PROCESS_ABORTAR_TRANSACCION = "mobilePosAbortarTransaccion"
        const val PROCESS_APERTURA_CAJA = "mobilePosAperturaCaja"
        const val PROCESS_BUSCAR_CLIENTE = "mobilePosBuscarCliente"
        const val PROCESS_CAJA = "mobilePosCaja"
        const val PROCESS_CAMBIAR_CANTIDAD_ARTICULO = "mobilePosCambiarCantidadArticulo"
        const val PROCESS_CATALOGO_DIGITAL = "mobilePosCatalogoDigital"
        const val PROCESS_CIERRE_CAJA = "mobilePosCierreCaja"
        const val PROCESS_CIERRE_DATAFONO = "mobilePosCierreDatafono"
        const val PROCESS_COMUN = "mobilePosComun"
        const val PROCESS_ELIMINAR_LINEA = "mobilePosEliminarLinea"
        const val PROCESS_ENVASES = "mobilePosEnvases"
        const val PROCESS_FACTURAR = "mobilePosFacturar"
        const val PROCESS_OPCIONES_AVANZADAS = "mobilePosOpcionesAvanzadas"
        const val PROCESS_REIMPRESION = "mobilePosReimpresion"
        const val PROCESS_TRANSACCION = "mobilePosTransaccion"
        const val PROCESS_TRANSACCION_EN_ESPERA = "mobilePosTransaccionEnEspera"
        const val PROCESS_AUTORIZAR_MATERIAL_RESTRINGIDO = "mobilePosAutorizarMaterialRestringido"
    }
}

data class ProcessPermission(
    val access: Boolean = false,
    val show: Boolean = false
)

data class ScreenAccess(
    val systemCode: Int = 0,
    val groupCode: String = "",
    val screenAlias: String = "",
    val screenEnter: Int = 0,
    val screenInclude: Int = 0,
    val screenCopy: Int = 0,
    val screenModify: Int = 0,
    val screenDelete: Int = 0,
    val screenNavigate: Int = 0,
    val screenSearch: Int = 0,
    val screenPrint: Int = 0,
    val screenRefresh: Int = 0,
    val screenHelp: Int = 0,
    val screenQuery: Int = 0
)
