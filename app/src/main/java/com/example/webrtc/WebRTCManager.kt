package com.example.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    private val signalingClient: SignalingClient
) {
    private val eglBase = EglBase.create()
    private val factory: PeerConnectionFactory

    private var peerConnection: PeerConnection? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null

    var localVideoTrack: VideoTrack? = null
        private set
    var localAudioTrack: AudioTrack? = null
        private set
    
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState.asStateFlow()

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun getEglBaseContext(): EglBase.Context = eglBase.eglBaseContext

    fun initializeLocalMedia() {
        // 1. Create Video Capturer (Camera)
        videoCapturer = createVideoCapturer()
        
        // 2. Create Video Source & Track
        localVideoSource = factory.createVideoSource(videoCapturer?.isScreencast == true)
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = factory.createVideoTrack("local_video", localVideoSource)

        // 3. Create Audio Source & Track
        val audioConstraints = MediaConstraints()
        localAudioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack = factory.createAudioTrack("local_audio", localAudioSource)
    }

    fun startCall(remoteUserId: String) {
        setupPeerConnection(remoteUserId)
        
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let {
                    peerConnection?.setLocalDescription(this, it)
                    signalingClient.sendOffer(remoteUserId, it.description)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { Log.e(TAG, "Failed to create offer: $error") }
            override fun onSetFailure(error: String?) { Log.e(TAG, "Failed to set offer: $error") }
        }, constraints)
    }

    fun handleOffer(remoteUserId: String, sdpDescription: String) {
        setupPeerConnection(remoteUserId)
        
        val remoteSdp = SessionDescription(SessionDescription.Type.OFFER, sdpDescription)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                // Create Answer
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                        sessionDescription?.let {
                            peerConnection?.setLocalDescription(this, it)
                            signalingClient.sendAnswer(remoteUserId, it.description)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) { Log.e(TAG, "Failed to create answer: $error") }
                    override fun onSetFailure(error: String?) { Log.e(TAG, "Failed to set answer: $error") }
                }, MediaConstraints())
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) { Log.e(TAG, "Failed to set remote offer: $error") }
        }, remoteSdp)
    }

    fun handleAnswer(sdpDescription: String) {
        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpDescription)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) { Log.e(TAG, "Failed to set remote answer: $error") }
        }, remoteSdp)
    }

    fun handleIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate)
    }

    private fun setupPeerConnection(remoteUserId: String) {
        if (peerConnection != null) return

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    signalingClient.sendIceCandidate(remoteUserId, it.sdpMid, it.sdpMLineIndex, it.sdp)
                }
            }
            
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let { _connectionState.value = it }
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    _remoteVideoTrack.value = track
                }
            }
        })

        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("local_media")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("local_media")) }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        // Fallback to back camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun endCall() {
        videoCapturer?.tryStopCapture()
        videoCapturer?.dispose()
        localVideoSource?.dispose()
        localAudioSource?.dispose()
        peerConnection?.close()
        peerConnection = null
        _remoteVideoTrack.value = null
        _connectionState.value = PeerConnection.PeerConnectionState.CLOSED
    }

    private fun VideoCapturer.tryStopCapture() {
        try { stopCapture() } catch (e: InterruptedException) { Log.e(TAG, "Failed to stop capture", e) }
    }

    companion object {
        private const val TAG = "WebRTCManager"
    }
}
