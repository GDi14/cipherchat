package com.example.ui.conversation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseChatRepository
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.LocalContactEntity
import com.example.data.local.entity.LocalMessageEntity
import com.example.ui.conversation.audio.AudioPlaybackState
import com.example.ui.conversation.audio.AudioPlayerManager
import com.example.ui.conversation.audio.VoiceRecorderManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ConversationViewModel(
    val friendUserId: String,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val chatRepository: FirebaseChatRepository
) : ViewModel() {

    val messages: StateFlow<List<LocalMessageEntity>> = messageDao.getMessagesForConversation(friendUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _friendContact = MutableStateFlow<LocalContactEntity?>(null)
    val friendContact: StateFlow<LocalContactEntity?> = _friendContact.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<LocalMessageEntity?>(null)
    val replyingToMessage: StateFlow<LocalMessageEntity?> = _replyingToMessage.asStateFlow()

    // Voice Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private var voiceRecorderManager: VoiceRecorderManager? = null
    private var recordingJob: Job? = null

    // Audio Playback Manager & States
    private var audioPlayerManager: AudioPlayerManager? = null
    val audioPlaybackState: StateFlow<AudioPlaybackState>
        get() = audioPlayerManager?.playbackState ?: MutableStateFlow(AudioPlaybackState())

    // Typing Indicators State
    private val _friendIsTyping = MutableStateFlow(false)
    val friendIsTyping: StateFlow<Boolean> = _friendIsTyping.asStateFlow()

    private var typingListener: ListenerRegistration? = null
    private var typingDebounceJob: Job? = null

    init {
        chatRepository.activeChatUserId = friendUserId
        loadFriendInfo()
        markMessagesAsRead()
    }

    override fun onCleared() {
        super.onCleared()
        if (chatRepository.activeChatUserId == friendUserId) {
            chatRepository.activeChatUserId = null
        }
        typingListener?.remove()
        typingDebounceJob?.cancel()
        voiceRecorderManager?.cancelRecording()
        audioPlayerManager?.release()
    }

    fun startListeningToTyping(currentUserId: String) {
        typingListener?.remove()
        typingListener = chatRepository.listenToTypingStatus(friendUserId, currentUserId) { isTyping ->
            _friendIsTyping.value = isTyping
        }
    }

    fun onUserTyping(currentUserId: String, text: String) {
        typingDebounceJob?.cancel()
        if (text.isNotBlank()) {
            chatRepository.setTypingStatus(currentUserId, friendUserId, true)
            typingDebounceJob = viewModelScope.launch {
                delay(3000)
                chatRepository.setTypingStatus(currentUserId, friendUserId, false)
            }
        } else {
            chatRepository.setTypingStatus(currentUserId, friendUserId, false)
        }
    }

    fun loadFriendInfo() {
        viewModelScope.launch {
            _friendContact.value = contactDao.getContactById(friendUserId)
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            chatRepository.markChatAsRead(friendUserId)
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun setReplyingTo(message: LocalMessageEntity) {
        _replyingToMessage.value = message
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val replyTarget = _replyingToMessage.value
        val replySenderName = when {
            replyTarget == null -> null
            replyTarget.isOutgoing -> "You"
            else -> _friendContact.value?.displayName ?: "Friend"
        }

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            val result = chatRepository.sendTextMessage(
                recipientUserId = friendUserId,
                text = trimmed,
                replyToMessageId = replyTarget?.id,
                replyToSenderName = replySenderName,
                replyToText = replyTarget?.plaintext
            )
            if (result.isSuccess) {
                _replyingToMessage.value = null
            } else {
                val err = result.exceptionOrNull()
                _errorMessage.value = err?.localizedMessage ?: "Failed to send message"
            }
            _isSending.value = false
        }
    }

    fun sendAttachment(
        uri: Uri,
        fileName: String,
        mimeType: String,
        caption: String = ""
    ) {
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            val result = chatRepository.sendAttachmentMessage(
                recipientUserId = friendUserId,
                uri = uri,
                fileName = fileName,
                mimeType = mimeType,
                caption = caption
            )

            if (result.isFailure) {
                val err = result.exceptionOrNull()
                _errorMessage.value = err?.localizedMessage
                    ?: "File sharing isn't set up yet — enable Firebase Storage in the console"
            }
            _isSending.value = false
        }
    }

    // --- Voice Recording Logic ---

    fun startVoiceRecording(context: Context) {
        if (voiceRecorderManager == null) {
            voiceRecorderManager = VoiceRecorderManager(context.applicationContext)
        }

        val success = voiceRecorderManager?.startRecording() ?: false
        if (success) {
            _isRecording.value = true
            _recordingDurationSeconds.value = 0

            recordingJob?.cancel()
            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingDurationSeconds.value += 1
                    if (_recordingDurationSeconds.value >= 180) { // 3 min cap
                        stopAndSendVoiceRecording(context)
                        break
                    }
                }
            }
        } else {
            _errorMessage.value = "Failed to access microphone or start recording"
        }
    }

    fun stopAndSendVoiceRecording(context: Context) {
        if (!_isRecording.value) return
        _isRecording.value = false
        recordingJob?.cancel()

        val audioFile = voiceRecorderManager?.stopRecording()
        val duration = _recordingDurationSeconds.value

        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            val uri = Uri.fromFile(audioFile)
            val minutes = duration / 60
            val seconds = duration % 60
            val formattedDuration = String.format("%d:%02d", minutes, seconds)

            sendAttachment(
                uri = uri,
                fileName = "VoiceMemo_$formattedDuration.m4a",
                mimeType = "audio/m4a",
                caption = "Voice Memo ($formattedDuration)"
            )
        } else {
            _errorMessage.value = "Voice recording was empty or failed"
        }
    }

    fun cancelVoiceRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        voiceRecorderManager?.cancelRecording()
        _recordingDurationSeconds.value = 0
    }

    // --- Audio Playback Logic ---

    fun playOrPauseVoiceMemo(context: Context, messageId: String, localPath: String?) {
        if (localPath.isNullOrEmpty()) {
            _errorMessage.value = "Voice memo is still downloading or unavailable"
            return
        }

        val file = File(localPath)
        if (!file.exists()) {
            _errorMessage.value = "Voice memo file not found on device"
            return
        }

        if (audioPlayerManager == null) {
            audioPlayerManager = AudioPlayerManager(context.applicationContext)
        }

        audioPlayerManager?.playAudio(messageId, file)
    }

    class Factory(
        private val friendUserId: String,
        private val messageDao: MessageDao,
        private val contactDao: ContactDao,
        private val chatRepository: FirebaseChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(friendUserId, messageDao, contactDao, chatRepository) as T
        }
    }
}
