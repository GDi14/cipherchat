package com.example.webrtc

import kotlinx.serialization.Serializable

@Serializable
sealed class SignalingMessage {
    abstract val senderId: String
    abstract val recipientId: String

    @Serializable
    data class Offer(
        override val senderId: String,
        override val recipientId: String,
        val sdp: String
    ) : SignalingMessage()

    @Serializable
    data class Answer(
        override val senderId: String,
        override val recipientId: String,
        val sdp: String
    ) : SignalingMessage()

    @Serializable
    data class IceCandidate(
        override val senderId: String,
        override val recipientId: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val sdp: String
    ) : SignalingMessage()
    
    @Serializable
    data class EndCall(
        override val senderId: String,
        override val recipientId: String
    ) : SignalingMessage()

    @Serializable
    data class RejectCall(
        override val senderId: String,
        override val recipientId: String
    ) : SignalingMessage()

    @Serializable
    data class AcceptCall(
        override val senderId: String,
        override val recipientId: String
    ) : SignalingMessage()

    @Serializable
    data class CallTimeout(
        override val senderId: String,
        override val recipientId: String
    ) : SignalingMessage()
}
