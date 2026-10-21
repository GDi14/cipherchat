package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.repository.ChatRepository
import com.example.util.AudioPlayerHelper
import com.example.util.AudioRecorderHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val sendingMediaUri: Uri? = null,
    val sendingMediaType: String? = null,
    val errorMessage: String? = null,
    val scrollToMessageId: String? = null,
    val initialUnreadCount: Int = 0
)

class ChatViewModel(
    private val context: Context,
    val conversationId: String,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val myUserId: String = chatRepository.myUserId
    val myDisplayName: String get() = chatRepository.getMyDisplayName()

    val messages: Flow<PagingData<MessageEntity>> =
        chatRepository.getPagedMessagesForConversation(conversationId)
            .cachedIn(viewModelScope)

    private val _pendingMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val pendingMessages: StateFlow<List<MessageEntity>> = _pendingMessages.asStateFlow()

    val typingUsers: StateFlow<Map<String, Set<String>>> = chatRepository.typingUsers
    val voiceOfUserEnabled: StateFlow<Boolean> = chatRepository.voiceOfUserEnabled
    val emphasisAnimationMessageIds: StateFlow<Set<String>> = chatRepository.emphasisAnimationMessageIds

    fun completeEmphasisAnimation(messageId: String) = chatRepository.completeEmphasisAnimation(messageId)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorderHelper(context)
    private val audioPlayer = AudioPlayerHelper()

    private var voiceTimerJob: Job? = null
    private var typingTimerJob: Job? = null
    private var typingIsPublished = false
    private var isLoadingOlderMessages = false
    private var hasMoreOlderMessages = true

    fun loadOlderMessages() {
        // Handled automatically by Paging 3
    }

    init {
        loadConversation()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            val conv = chatRepository.getConversation(conversationId)
            val unread = conv?.unreadCount ?: 0
            _uiState.value = _uiState.value.copy(
                conversation = conv,
                initialUnreadCount = unread
            )
        }
    }

    fun markRead() {
        chatRepository.activateConversation(conversationId)
        viewModelScope.launch {
            // Reconcile with the relay on every foreground entry so a dropped
            // realtime subscription never leaves the user waiting for refresh.
            chatRepository.syncAllFromSupabase()
            chatRepository.markConversationAsRead(conversationId)
        }
    }

    fun onConversationHidden() {
        chatRepository.closeActiveConversation(conversationId)
        chatRepository.clearEmphasisAnimation(emptyList())
        typingTimerJob?.cancel()
        typingIsPublished = false
        viewModelScope.launch { chatRepository.setTyping(conversationId, false) }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, errorMessage = null)

        // Typing notification
        typingTimerJob?.cancel()
        if (text.isNotBlank() && !typingIsPublished) {
            typingIsPublished = true
            viewModelScope.launch { chatRepository.setTyping(conversationId, true) }
        }
        typingTimerJob = viewModelScope.launch {
            delay(3000)
            if (typingIsPublished) {
                typingIsPublished = false
                chatRepository.setTyping(conversationId, false)
            }
        }
    }

    fun setReplyingTo(message: MessageEntity?) {
        _uiState.value = _uiState.value.copy(replyingTo = message)
    }

    fun hideMessage(message: MessageEntity) {
        viewModelScope.launch {
            runCatching { chatRepository.hideMessageLocally(message.id) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.localizedMessage ?: "Couldn't delete this message"
                    )
                }
        }
    }

    fun sendTextMessage(onProvisionalCreated: (MessageEntity) -> Unit = {}): MessageEntity? {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return null
        val replyTo = _uiState.value.replyingTo
        val timestamp = System.currentTimeMillis()
        val provisional = MessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = myUserId,
            senderName = myDisplayName,
            recipientId = _uiState.value.conversation?.otherUserId,
            plaintextBody = text,
            replyToId = replyTo?.id,
            replySnippet = replyTo?.let { if (it.plaintextBody.isNotBlank()) it.plaintextBody.take(60) else "[Media]" },
            status = "sending",
            timestamp = timestamp
        )

        // Let the screen register the flight proxy before publishing the real
        // row, so Compose cannot draw both during the same send handoff.
        onProvisionalCreated(provisional)

        // Publish the row synchronously so Compose can lay it out before the
        // repository starts encryption or any network work.
        _pendingMessages.update { it + provisional }
        
        _uiState.value = _uiState.value.copy(
            inputText = "",
            replyingTo = null,
            errorMessage = null
        )

        viewModelScope.launch {
            chatRepository.setTyping(conversationId, false)
            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                text = text,
                replyToMessage = replyTo,
                messageId = provisional.id,
                timestamp = provisional.timestamp
            )
            result.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err.localizedMessage ?: "Failed to send encrypted message"
                )
            }
        }
        return provisional
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
            val replyTo = _uiState.value.replyingTo
            _uiState.value = _uiState.value.copy(isSending = true)
            viewModelScope.launch {
                val sendResult = chatRepository.sendMediaMessage(
                    conversationId = conversationId,
                    file = file,
                    mimeType = "audio/m4a",
                    caption = "",
                    durationSeconds = duration,
                    replyToMessage = replyTo
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
        val replyTo = _uiState.value.replyingTo
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, sendingMediaUri = uri,
                sendingMediaType = mimeType, errorMessage = null)
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

                // Replace the picker preview with the persisted pending bubble
                // before the slower encryption and upload work begins.
                _uiState.value = _uiState.value.copy(sendingMediaUri = null, sendingMediaType = null)

                val sendRes = chatRepository.sendMediaMessage(
                    conversationId = conversationId,
                    file = tempFile,
                    mimeType = mimeType,
                    replyToMessage = replyTo
                )
                _uiState.value = _uiState.value.copy(isSending = false, sendingMediaUri = null,
                    sendingMediaType = null, replyingTo = null)
                sendRes.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Media send failed"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    sendingMediaUri = null,
                    sendingMediaType = null,
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

    suspend fun peerFingerprint(peerId: String): String? = chatRepository.peerFingerprint(peerId)

    fun resetSessionWithPeer(peerId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.resetSession(peerId)
            result.onSuccess {
                onResult(true, "Secure session successfully reset!")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Failed to reset session")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.closeActiveConversation(conversationId)
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
