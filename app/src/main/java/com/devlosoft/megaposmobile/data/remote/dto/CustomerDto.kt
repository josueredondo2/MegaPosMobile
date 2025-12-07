package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.Customer
import com.google.gson.annotations.SerializedName

data class CustomerDto(
    @SerializedName("partyId")
    val partyId: Int,

    @SerializedName("partyType")
    val partyType: String?,

    @SerializedName("identification")
    val identification: String,

    @SerializedName("identificationDescription")
    val identificationDescription: String?,

    @SerializedName("identificationType")
    val identificationType: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("affiliate")
    val affiliate: String?,

    @SerializedName("affiliateType")
    val affiliateType: String?,

    @SerializedName("discountAmount")
    val discountAmount: Double?,

    @SerializedName("percentageDiscount")
    val percentageDiscount: Double?,

    @SerializedName("isIdentificationValidForFEL")
    val isIdentificationValidForFEL: Boolean?,

    @SerializedName("doesReceiveFEL")
    val doesReceiveFEL: Boolean?,

    @SerializedName("statusCode")
    val statusCode: String?,

    @SerializedName("isValid")
    val isValid: Boolean?,

    @SerializedName("requiresIdentification")
    val requiresIdentification: Boolean?,

    @SerializedName("balance")
    val balance: Double?,

    @SerializedName("creditLimit")
    val creditLimit: Double?,

    @SerializedName("gender")
    val gender: String?
) {
    fun toDomain(): Customer = Customer(
        partyId = partyId,
        partyType = partyType ?: "",
        identification = identification,
        identificationDescription = identificationDescription ?: "",
        identificationType = identificationType ?: "",
        name = name,
        affiliate = affiliate ?: "",
        affiliateType = affiliateType ?: "",
        discountAmount = discountAmount ?: 0.0,
        percentageDiscount = percentageDiscount ?: 0.0,
        isValid = isValid ?: true
    )
}
