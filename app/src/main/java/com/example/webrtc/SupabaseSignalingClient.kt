package com.example.webrtc

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SupabaseSignalingClient(
    private val supabase: SupabaseClient,
    private val myUserId: String,
    private val callId: String,
    private val coroutineScope: CoroutineScope
) : SignalingClient {

    private var channel: RealtimeChannel? = null
    private var job: Job? = null

    private val _incomingOffers = MutableSharedFlow<SignalingMessage.Offer>()
    val incomingOffers: SharedFlow<SignalingMessage.Offer> = _incomingOffers.asSharedFlow()

    private val _incomingAnswers = MutableSharedFlow<SignalingMessage.Answer>()
    val incomingAnswers: SharedFlow<SignalingMessage.Answer> = _incomingAnswers.asSharedFlow()

    private val _incomingIceCandidates = MutableSharedFlow<SignalingMessage.IceCandidate>()
    val incomingIceCandidates: SharedFlow<SignalingMessage.IceCandidate> = _incomingIceCandidates.asSharedFlow()

    private val _incomingEndCall = MutableSharedFlow<SignalingMessage.EndCall>()
    val incomingEndCall: SharedFlow<SignalingMessage.EndCall> = _incomingEndCall.asSharedFlow()

    private val _incomingRejections = MutableSharedFlow<SignalingMessage.RejectCall>()
    val incomingRejections: SharedFlow<SignalingMessage.RejectCall> = _incomingRejections.asSharedFlow()

    private val _incomingAcceptances = MutableSharedFlow<SignalingMessage.AcceptCall>()
    val incomingAcceptances: SharedFlow<SignalingMessage.AcceptCall> = _incomingAcceptances.asSharedFlow()

    private val _incomingTimeouts = MutableSharedFlow<SignalingMessage.CallTimeout>()
    val incomingTimeouts: SharedFlow<SignalingMessage.CallTimeout> = _incomingTimeouts.asSharedFlow()

    fun connect() {
        if (channel != null) return

        channel = supabase.channel("call_$callId")
        
        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.Offer>(event = "offer")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received Offer")
                    _incomingOffers.emit(message)
                }
            }
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.Answer>(event = "answer")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received Answer")
                    _incomingAnswers.emit(message)
                }
            }
        }
        
        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.IceCandidate>(event = "ice_candidate")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received ICE Candidate")
                    _incomingIceCandidates.emit(message)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.EndCall>(event = "end_call")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received End Call")
                    _incomingEndCall.emit(message)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.RejectCall>(event = "reject_call")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received Reject Call")
                    _incomingRejections.emit(message)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.AcceptCall>(event = "accept_call")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received Accept Call")
                    _incomingAcceptances.emit(message)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            channel?.broadcastFlow<SignalingMessage.CallTimeout>(event = "call_timeout")?.collect { message ->
                if (message.recipientId == myUserId) {
                    Log.d(TAG, "Received Call Timeout")
                    _incomingTimeouts.emit(message)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                channel?.subscribe(blockUntilSubscribed = true)
                Log.d(TAG, "Subscribed to signaling channel call_$callId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to signaling channel", e)
            }
        }
    }

    override fun sendOffer(remoteUserId: String, sdp: String) {
        val message = SignalingMessage.Offer(senderId = myUserId, recipientId = remoteUserId, sdp = sdp)
        broadcastMessage("offer", message)
    }

    override fun sendAnswer(remoteUserId: String, sdp: String) {
        val message = SignalingMessage.Answer(senderId = myUserId, recipientId = remoteUserId, sdp = sdp)
        broadcastMessage("answer", message)
    }

    override fun sendIceCandidate(remoteUserId: String, sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val message = SignalingMessage.IceCandidate(
            senderId = myUserId, 
            recipientId = remoteUserId, 
            sdpMid = sdpMid, 
            sdpMLineIndex = sdpMLineIndex, 
            sdp = sdp
        )
        broadcastMessage("ice_candidate", message)
    }
    
    fun sendEndCall(remoteUserId: String) {
        val message = SignalingMessage.EndCall(senderId = myUserId, recipientId = remoteUserId)
        broadcastMessage("end_call", message)
    }

    fun sendRejectCall(remoteUserId: String) {
        val message = SignalingMessage.RejectCall(senderId = myUserId, recipientId = remoteUserId)
        broadcastMessage("reject_call", message)
    }

    fun sendAcceptCall(remoteUserId: String) {
        val message = SignalingMessage.AcceptCall(senderId = myUserId, recipientId = remoteUserId)
        broadcastMessage("accept_call", message)
    }

    fun sendCallTimeout(remoteUserId: String) {
        val message = SignalingMessage.CallTimeout(senderId = myUserId, recipientId = remoteUserId)
        broadcastMessage("call_timeout", message)
    }

    private inline fun <reified T : SignalingMessage> broadcastMessage(event: String, message: T) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                channel?.broadcast(event, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast $event", e)
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                channel?.unsubscribe()
                channel = null
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing", e)
            }
        }
    }

    companion object {
        private const val TAG = "SupabaseSignalingClient"
    }
}
