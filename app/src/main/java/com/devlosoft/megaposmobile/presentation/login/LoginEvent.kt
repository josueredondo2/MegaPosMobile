package com.devlosoft.megaposmobile.presentation.login

sealed class LoginEvent {
    data class UserCodeChanged(val code: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object TogglePasswordVisibility : LoginEvent()
    data object Login : LoginEvent()
    data object ClearError : LoginEvent()
}
