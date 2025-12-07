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
}
