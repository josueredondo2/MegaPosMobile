package com.devlosoft.megaposmobile.domain.model

data class Customer(
    val partyId: Int,
    val partyType: String,
    val identification: String,
    val identificationDescription: String,
    val identificationType: String,
    val name: String,
    val affiliate: String,
    val affiliateType: String,
    val discountAmount: Double,
    val percentageDiscount: Double,
    val isValid: Boolean
) {
    fun getInitial(): String {
        return name.firstOrNull()?.uppercase() ?: "?"
    }

    companion object {
        /**
         * Default customer used when no customer is selected.
         * This customer is used locally without calling the backend.
         */
        val DEFAULT = Customer(
            partyId = 1478105,
            partyType = "PERS",
            identification = "2116",
            identificationDescription = "Cédula de identidad.",
            identificationType = "CI",
            name = "CLIENTE GENERAL",
            affiliate = "Cliente General",
            affiliateType = "0001",
            discountAmount = 0.0,
            percentageDiscount = 0.0,
            isValid = true
        )
    }
}
