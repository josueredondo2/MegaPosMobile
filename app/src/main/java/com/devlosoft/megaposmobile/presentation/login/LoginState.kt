package com.devlosoft.megaposmobile.presentation.login

data class LoginState(
    val userCode: String = "304720192",
    val password: String = "304720192",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false
)
