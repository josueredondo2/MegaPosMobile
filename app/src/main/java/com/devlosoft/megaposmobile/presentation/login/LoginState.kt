package com.devlosoft.megaposmobile.presentation.login

data class LoginState(
    val userCode: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false
)
