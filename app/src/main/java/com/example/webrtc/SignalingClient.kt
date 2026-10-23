package com.example.webrtc

interface SignalingClient {
    /**
     * Send an SDP Offer to a remote user.
     */
    fun sendOffer(remoteUserId: String, sdp: String)

    /**
     * Send an SDP Answer to a remote user.
     */
    fun sendAnswer(remoteUserId: String, sdp: String)

    /**
     * Send an ICE Candidate to a remote user.
     */
    fun sendIceCandidate(remoteUserId: String, sdpMid: String, sdpMLineIndex: Int, sdp: String)
}
