package com.hoqit.beea.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AssistantEvent {
    data class WakeWordDetected(val modelName: String) : AssistantEvent()
    data class TtsStarted(val text: String) : AssistantEvent()
    object TtsFinished : AssistantEvent()
}

object AssistantState {
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _events = MutableSharedFlow<AssistantEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun setListening(value: Boolean) {
        _isListening.value = value
    }

    suspend fun emit(event: AssistantEvent) {
        _events.emit(event)
    }
}