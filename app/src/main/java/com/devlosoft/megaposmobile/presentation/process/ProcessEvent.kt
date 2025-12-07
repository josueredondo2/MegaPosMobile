package com.devlosoft.megaposmobile.presentation.process

sealed class ProcessEvent {
    data object GoBack : ProcessEvent()
    data object RetryProcess : ProcessEvent()
}
