package com.devlosoft.megaposmobile.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class StationState {
    OPEN,
    CLOSED
}

@Singleton
class StationStatus @Inject constructor() {

    private val _state = MutableStateFlow(StationState.CLOSED)
    val state: StateFlow<StationState> = _state.asStateFlow()

    fun open() {
        _state.value = StationState.OPEN
    }

    fun close() {
        _state.value = StationState.CLOSED
    }

    fun getDisplayText(): String {
        return when (_state.value) {
            StationState.OPEN -> "Abierto"
            StationState.CLOSED -> "Cerrado"
        }
    }
}
