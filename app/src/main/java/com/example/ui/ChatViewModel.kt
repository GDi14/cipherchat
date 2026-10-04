package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.repository.ChatRepository
import com.example.util.AudioPlayerHelper
import com.example.util.AudioRecorderHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ChatUiState(
    val conversation: ConversationEntity? = null,
    val inputText: String = "",
    val replyingTo: MessageEntity? = null,
    val isRecordingVoice: Boolean = false,
    val voiceDurationSeconds: Int = 0,
    val playingAudioMessageId: String? = null,
    val audioProgress: Float = 0f,
    val audioCurrentSeconds: Int = 0,
    val isSending: Boolean = false,
    val selectedViewerMedia: MessageEntity? = null,
    val errorMessage: String? = null,
    val scrollToMessageId: String? = null
)

class ChatViewModel(
    private val context: Context,
    val conversationId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val myUserId: String = chatRepository.myUserId

    val messages: StateFlow<List<MessageEntity>> = chatRepository.getMessagesForConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val typingUsers: StateFlow<Map<String, Set<String>>> = chatRepository.typingUsers

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorderHelper(context)
    private val audioPlayer = AudioPlayerHelper()

    private var voiceTimerJob: Job? = null
    private var typingTimerJob: Job? = null

    init {
        loadConversation()
        markRead()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conv = chatRepository.getConversation(conversationId)
            _uiState.value = _uiState.value.copy(conversation = conv)
        }
    }

    fun markRead() {
        viewModelScope.launch {
            chatRepository.markConversationAsRead(conversationId)
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null)

        // Typing notification
        typingTimerJob?.cancel()
        typingTimerJob = viewModelScope.launch {
            chatRepository.setTyping(conversationId, true)
            delay(3000)
            chatRepository.setTyping(conversationId, false)
        }
    }

    fun setReplyingTo(message: MessageEntity?) {
        _uiState.value = _uiState.value.copy(replyingTo = message)
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        val replyTo = _uiState.value.replyingTo

        _uiState.value = _uiState.value.copy(
            inputText = "",
            replyingTo = null,
            isSending = true,
            errorMessage = null
        )

        viewModelScope.launch {
            chatRepository.setTyping(conversationId, false)
            val result = chatRepository.sendMessage(conversationId, text, replyTo)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = err.localizedMessage ?: "Failed to send encrypted message"
                )
            }
        }
    }

    fun startVoiceRecording(): Boolean {
        audioPlayer.stop()
        val started = audioRecorder.startRecording()
        if (started) {
            _uiState.value = _uiState.value.copy(
                isRecordingVoice = true,
                voiceDurationSeconds = 0
            )
            voiceTimerJob?.cancel()
            voiceTimerJob = viewModelScope.launch {
                var sec = 0
                while (audioRecorder.isRecording) {
                    delay(1000)
                    sec++
                    _uiState.value = _uiState.value.copy(voiceDurationSeconds = sec)
                }
            }
        }
        return started
    }

    fun stopAndSendVoiceRecording() {
        voiceTimerJob?.cancel()
        val result = audioRecorder.stopRecording()
        _uiState.value = _uiState.value.copy(isRecordingVoice = false, voiceDurationSeconds = 0)

        if (result != null) {
            val (file, duration) = result
            _uiState.value = _uiState.value.copy(isSending = true)
            viewModelScope.launch {
                val sendResult = chatRepository.sendMediaMessage(
                    conversationId = conversationId,
                    file = file,
                    mimeType = "audio/m4a",
                    caption = "",
                    durationSeconds = duration,
                    replyToMessage = _uiState.value.replyingTo
                )
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    replyingTo = null
                )
                sendResult.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Voice note failed to send"
                    )
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        voiceTimerJob?.cancel()
        audioRecorder.cancelRecording()
        _uiState.value = _uiState.value.copy(isRecordingVoice = false, voiceDurationSeconds = 0)
    }

    fun sendMediaUri(uri: Uri, mimeType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val ext = when {
                    mimeType.startsWith("video") -> "mp4"
                    else -> "jpg"
                }
                val tempFile = File(context.cacheDir, "send_media_${System.currentTimeMillis()}.$ext")
                inputStream?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val sendRes = chatRepository.sendMediaMessage(
                    conversationId = conversationId,
                    file = tempFile,
                    mimeType = mimeType,
                    replyToMessage = _uiState.value.replyingTo
                )
                _uiState.value = _uiState.value.copy(isSending = false, replyingTo = null)
                sendRes.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Media send failed"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = e.localizedMessage ?: "Could not process media"
                )
            }
        }
    }

    fun playAudio(message: MessageEntity) {
        val path = message.localDecryptedPath
        if (path != null && File(path).exists()) {
            startAudioPlayback(message.id, path)
        } else {
            // Need to download and decrypt first
            viewModelScope.launch {
                val res = chatRepository.downloadAndDecryptMedia(message)
                res.onSuccess { localPath ->
                    startAudioPlayback(message.id, localPath)
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Failed to download voice note"
                    )
                }
            }
        }
    }

    private fun startAudioPlayback(messageId: String, path: String) {
        if (_uiState.value.playingAudioMessageId == messageId && audioPlayer.isPlaying(path)) {
            audioPlayer.pause()
            _uiState.value = _uiState.value.copy(playingAudioMessageId = null)
            return
        }

        _uiState.value = _uiState.value.copy(
            playingAudioMessageId = messageId,
            audioProgress = 0f,
            audioCurrentSeconds = 0
        )
        audioPlayer.play(
            path = path,
            onProgress = { progress, sec ->
                if (_uiState.value.playingAudioMessageId == messageId) {
                    _uiState.value = _uiState.value.copy(
                        audioProgress = progress,
                        audioCurrentSeconds = sec
                    )
                }
            },
            onCompletion = {
                if (_uiState.value.playingAudioMessageId == messageId) {
                    _uiState.value = _uiState.value.copy(
                        playingAudioMessageId = null,
                        audioProgress = 0f,
                        audioCurrentSeconds = 0
                    )
                }
            }
        )
    }

    fun openMediaViewer(message: MessageEntity) {
        if (message.localDecryptedPath != null && File(message.localDecryptedPath).exists()) {
            _uiState.value = _uiState.value.copy(selectedViewerMedia = message)
        } else {
            viewModelScope.launch {
                val res = chatRepository.downloadAndDecryptMedia(message)
                res.onSuccess {
                    _uiState.value = _uiState.value.copy(selectedViewerMedia = message)
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Failed to decrypt media"
                    )
                }
            }
        }
    }

    fun closeMediaViewer() {
        _uiState.value = _uiState.value.copy(selectedViewerMedia = null)
    }

    fun jumpToMessage(messageId: String) {
        _uiState.value = _uiState.value.copy(scrollToMessageId = messageId)
    }

    fun clearScrollToMessage() {
        _uiState.value = _uiState.value.copy(scrollToMessageId = null)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        audioRecorder.cancelRecording()
    }

    class Factory(
        private val context: Context,
        private val conversationId: String,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(context, conversationId, chatRepository) as T
        }
    }
}
