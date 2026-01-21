package com.devlosoft.megaposmobile.core.constants

/**
 * Field length constants matching SQL Server database column constraints.
 * These values should be used to limit text input fields to prevent truncation errors.
 */
object FieldLengths {
    /** ID_ITM_PS - Article/barcode code */
    const val ARTICLE_CODE = 14

    /** seg_usuarios.usu_codigo - User code */
    const val USER_CODE = 15

    /** TR_RTL.Z_TRN_CT_NM - Customer name */
    const val CUSTOMER_NAME = 255

    /** TR_RTL.CD_CT_ID_TYP - Customer ID type */
    const val CUSTOMER_ID_TYPE = 20

    /** seg_bitacora.bit_detalle - Abort/cancellation reason */
    const val ABORT_REASON = 1000

    /** seg_bitacora.bit_Hostname - Device hostname */
    const val HOSTNAME = 20

    /** TR_LTM_CRDB_CRD_TN.NM_CRD_HLD - Cardholder name */
    const val CARDHOLDER = 40

    /** TR_LTM_CRDB_CRD_TN.Z_NUM_TER - Terminal ID */
    const val TERMINAL_ID = 50

    /** TR_LTM_CRDB_CRD_TN.Z_NUM_INV - Receipt number */
    const val RECEIPT_NUMBER = 6

    /** TR_LTM_CRDB_CRD_TN.Z_COD_REFE - RRN (Retrieval Reference Number) */
    const val RRN = 40

    /** TR_LTM_CRDB_CRD_TN.Z_COD_TRCK - Stan */
    const val STAN = 40

    /** TR_LTM_CRDB_CRD_TN.CD_RCN - PAN masked */
    const val PAN_MASKED = 20

    /** TR_LTM_CRDB_CRD_TN.LU_ADJN_CRDB - Authorization code */
    const val AUTH_CODE = 6

    /** TR_RTL.CD_TYP_TRN_RTL - Transaction type */
    const val TRANSACTION_TYPE = 2
}
