package com.example.ui.call

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.SupabaseClientProvider
import com.example.webrtc.SupabaseSignalingClient
import com.example.webrtc.WebRTCManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack
import java.util.UUID

class CallViewModel(
    application: Application,
    val callId: String,
    val remoteUserId: String,
    val isCaller: Boolean
) : AndroidViewModel(application) {

    sealed interface CallSessionState {
        data object Ringing : CallSessionState
        data object Connecting : CallSessionState
        data object Connected : CallSessionState
        data class Ended(val reason: String) : CallSessionState
    }

    private val securePrefs = SecurePreferencesManager(application)
    private val supabaseProvider = SupabaseClientProvider(application, securePrefs)
    private val database = AppDatabase.getInstance(application)
    private val myUserId = securePrefs.getOrCreateUserId()

    var remoteUserName: String = "Friend"
        private set

    private val signalingClient = SupabaseSignalingClient(
        supabase = supabaseProvider.getClient(),
        myUserId = myUserId,
        callId = callId,
        coroutineScope = viewModelScope
    )

    private val webRTCManager = WebRTCManager(application, signalingClient)

    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = webRTCManager.connectionState
    val remoteVideoTrack: StateFlow<VideoTrack?> = webRTCManager.remoteVideoTrack
    val localVideoTrack: VideoTrack?
        get() = webRTCManager.localVideoTrack

    private val _callSessionState = MutableStateFlow<CallSessionState>(CallSessionState.Ringing)
    val callSessionState: StateFlow<CallSessionState> = _callSessionState.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private var timeoutJob: Job? = null
    private var callStartTimeMs: Long = 0L

    init {
        webRTCManager.initializeLocalMedia()
        signalingClient.connect()

        loadRemoteProfile()
        observeSignalingMessages()
        observeConnectionState()

        if (isCaller) {
            webRTCManager.startCall(remoteUserId)
            sendCallPushNotification()
            startTimeoutTimer()
        }
    }

    private fun loadRemoteProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = database.profileDao().getProfileById(remoteUserId)
            if (profile != null) {
                remoteUserName = profile.displayName
            }
        }
    }

    private fun startTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(30_000) // 30 seconds ringing timeout
            if (_callSessionState.value is CallSessionState.Ringing) {
                Log.d("CallViewModel", "Call timed out after 30 seconds")
                signalingClient.sendCallTimeout(remoteUserId)
                endCall("No answer")
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        timeoutJob?.cancel()
                        if (callStartTimeMs == 0L) callStartTimeMs = System.currentTimeMillis()
                        _callSessionState.value = CallSessionState.Connected
                    }
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        endCall("Disconnected")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun sendCallPushNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val okHttpClient = OkHttpClient()
                val jsonMedia = "application/json; charset=utf-8".toMediaType()
                val client = supabaseProvider.getClient()

                val userToken = runCatching {
                    client.auth.currentAccessTokenOrNull()
                }.getOrNull() ?: supabaseProvider.currentAnonKey

                val bodyObj = JSONObject().apply {
                    put("callee_id", remoteUserId)
                    put("caller_id", myUserId)
                    put("call_id", callId)
                    put("caller_name", securePrefs.getDisplayName())
                }

                val body = RequestBody.create(jsonMedia, bodyObj.toString())
                val url = "${supabaseProvider.currentUrl}/functions/v1/initiate-call"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseProvider.currentAnonKey)
                    .addHeader("Authorization", "Bearer $userToken")
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("CallViewModel", "Failed to send call push: ${response.code} - ${response.body?.string()}")
                    } else {
                        Log.d("CallViewModel", "Call push sent successfully: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error sending call push notification", e)
            }
        }
    }

    private fun observeSignalingMessages() {
        viewModelScope.launch {
            signalingClient.incomingOffers.collect { offer ->
                webRTCManager.handleOffer(offer.senderId, offer.sdp)
            }
        }

        viewModelScope.launch {
            signalingClient.incomingAnswers.collect { answer ->
                timeoutJob?.cancel()
                _callSessionState.value = CallSessionState.Connecting
                webRTCManager.handleAnswer(answer.sdp)
            }
        }

        viewModelScope.launch {
            signalingClient.incomingIceCandidates.collect { ice ->
                webRTCManager.handleIceCandidate(ice.sdpMid, ice.sdpMLineIndex, ice.sdp)
            }
        }

        viewModelScope.launch {
            signalingClient.incomingRejections.collect {
                endCall("Declined")
            }
        }

        viewModelScope.launch {
            signalingClient.incomingTimeouts.collect {
                endCall("No answer")
            }
        }

        viewModelScope.launch {
            signalingClient.incomingEndCall.collect {
                endCall("Ended")
            }
        }
    }

    fun getEglBaseContext(): EglBase.Context = webRTCManager.getEglBaseContext()

    fun toggleMic() {
        val muted = !_isMicMuted.value
        _isMicMuted.value = muted
        webRTCManager.localAudioTrack?.setEnabled(!muted)
    }

    fun toggleVideo() {
        val enabled = !_isVideoEnabled.value
        _isVideoEnabled.value = enabled
        webRTCManager.localVideoTrack?.setEnabled(enabled)
    }

    fun endCall(reason: String = "Ended") {
        if (_callSessionState.value is CallSessionState.Ended) return

        timeoutJob?.cancel()
        val durationSec = if (callStartTimeMs > 0L) {
            ((System.currentTimeMillis() - callStartTimeMs) / 1000).toInt()
        } else 0

        _callSessionState.value = CallSessionState.Ended(reason)

        signalingClient.sendEndCall(remoteUserId)
        signalingClient.disconnect()
        webRTCManager.endCall()

        recordCallLog(reason, durationSec)
    }

    private fun recordCallLog(reason: String, durationSec: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val statusText = when {
                durationSec > 0 -> {
                    val mins = durationSec / 60
                    val secs = durationSec % 60
                    val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                    "Video call · $timeStr"
                }
                reason == "No answer" -> "Missed video call"
                reason == "Declined" -> "Call declined"
                else -> "Video call ended"
            }

            val conv = database.conversationDao().getDirectConversation(remoteUserId)

            val conversationId = conv?.id ?: UUID.randomUUID().toString()
            if (conv == null) {
                database.conversationDao().insertOrUpdate(
                    ConversationEntity(
                        id = conversationId,
                        type = "direct",
                        title = remoteUserName,
                        otherUserId = remoteUserId,
                        lastMessageText = statusText,
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                )
            } else {
                database.conversationDao().updateLastMessage(conversationId, statusText, System.currentTimeMillis())
            }

            val messageEntity = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = myUserId,
                recipientId = remoteUserId,
                plaintextBody = "[Call] $statusText",
                messageType = 3,
                mediaType = "call",
                mediaDuration = durationSec,
                status = "sent",
                timestamp = System.currentTimeMillis()
            )
            database.messageDao().insertOrUpdate(messageEntity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endCall("Ended")
    }
}

class CallViewModelFactory(
    private val application: Application,
    private val callId: String,
    private val remoteUserId: String,
    private val isCaller: Boolean
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallViewModel(application, callId, remoteUserId, isCaller) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
