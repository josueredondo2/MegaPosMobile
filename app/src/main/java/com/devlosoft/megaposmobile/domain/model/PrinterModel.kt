package com.devlosoft.megaposmobile.domain.model

enum class PrinterModel(val displayName: String) {
    ZEBRA_ZQ511("Zebra - ZQ511");

    companion object {
        fun fromString(value: String): PrinterModel {
            return entries.find { it.name == value } ?: ZEBRA_ZQ511
        }
    }
}
