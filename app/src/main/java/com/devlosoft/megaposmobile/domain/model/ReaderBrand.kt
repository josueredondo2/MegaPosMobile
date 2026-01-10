package com.devlosoft.megaposmobile.domain.model

enum class ReaderBrand(val displayName: String) {
    ZEBRA("Zebra"),
    PAX("PAX");

    companion object {
        fun fromString(value: String): ReaderBrand {
            return entries.find { it.name == value } ?: ZEBRA
        }
    }
}
