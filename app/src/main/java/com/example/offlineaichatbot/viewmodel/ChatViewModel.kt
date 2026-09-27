package com.example.offlineaichatbot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineaichatbot.GemmaEngine
import com.example.offlineaichatbot.tools.ModelDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatViewModel(app : Application) : AndroidViewModel(app) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(value = emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(value = false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isReady = MutableStateFlow(value = false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _status = MutableStateFlow(value = "Preparing model...")
    val status: StateFlow<String> = _status.asStateFlow()

    private var gemmaEngine: GemmaEngine? = null

    init {
        viewModelScope.launch {
            try {
                _status.value = "Preparing model..."
                ModelDownloader.ensureModel(context = getApplication())
                _status.value = "Loading model..."
                gemmaEngine = GemmaEngine.create(context = getApplication())

                _status.value = "Model loaded"
                _isReady.value = true
            } catch (e : Exception) {
                _status.value = "Failed to load model ${e.message}"
            }
        }
    }

    fun sendMessage(message: String) {
        val engine = gemmaEngine ?: return
        if(_isGenerating.value) return
        viewModelScope.launch {
            _messages.value += ChatMessage(text = message, isUser = true)
            _messages.value += ChatMessage(text = "", isUser = false)
            _isGenerating.value = true

            try {
                engine.reply(message).collect {
                    val currentMessages = _messages.value.toMutableList()
                    val lastMessage = currentMessages.last()
                    currentMessages[currentMessages.size - 1] = lastMessage.copy(text = lastMessage.text.plus(it))
                _messages.value = currentMessages
                }
            }catch (e : Exception) {
                val currentMessages = _messages.value.toMutableList()
                val lastMessage = currentMessages.last()
                currentMessages[currentMessages.size - 1] = lastMessage.copy(text = lastMessage.text.plus("Error: ${e.message}"))
                _messages.value = currentMessages
            } finally {
                _isGenerating.value = false
            }
        }
    }
}