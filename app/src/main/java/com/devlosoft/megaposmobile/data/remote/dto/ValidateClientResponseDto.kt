package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ValidateClientResponseDto(
    @SerializedName("result")
    val result: Int,

    @SerializedName("resultDescription")
    val resultDescription: String?,

    @SerializedName("stackTrace")
    val stackTrace: String?,

    @SerializedName("requiredData")
    val requiredData: Boolean?,

    @SerializedName("exists")
    val exists: Boolean?,

    @SerializedName("clientAuthorizesEmail")
    val clientAuthorizesEmail: Boolean?,

    @SerializedName("clientEmail")
    val clientEmail: String?,

    @SerializedName("clientEmailValid")
    val clientEmailValid: Boolean?,

    @SerializedName("generalClient")
    val generalClient: Boolean?,

    @SerializedName("clientIdentification")
    val clientIdentification: String?,

    @SerializedName("clientIdentificationValid")
    val clientIdentificationValid: Boolean?,

    @SerializedName("clientName")
    val clientName: String?,

    @SerializedName("clientPartyId")
    val clientPartyId: Int?,

    @SerializedName("clientPhone")
    val clientPhone: String?,

    @SerializedName("clientPhoneValid")
    val clientPhoneValid: Boolean?,

    @SerializedName("clientType")
    val clientType: String?,

    @SerializedName("identificationTypeDetail")
    val identificationTypeDetail: String?,

    @SerializedName("identificationTypeAllowed")
    val identificationTypeAllowed: Boolean?,

    @SerializedName("identificationType")
    val identificationType: String?,

    @SerializedName("activityRequired")
    val activityRequired: Boolean?,

    @SerializedName("economicActivityErrorMessage")
    val economicActivityErrorMessage: String?,

    @SerializedName("clientNameValid")
    val clientNameValid: Boolean?,

    @SerializedName("activities")
    val activities: List<EconomicActivityDto>?
)
