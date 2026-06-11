package com.example.sharescreen.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcManager(private val context: Context) {

    companion object {
        private const val TAG = "WebRtcManager"
        private const val VIDEO_TRACK_ID = "screen_video"
        private const val STREAM_ID = "screen_stream"
        private const val CAPTURE_WIDTH = 1280
        private const val CAPTURE_HEIGHT = 720
        private const val CAPTURE_FPS = 30
    }

    val eglBase: EglBase = EglBase.create()

    private lateinit var peerConnectionFactory: PeerConnectionFactory

    // Screen capture (HOST only)
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null

    // One PeerConnection per remote peer (HOST has one per viewer, VIEWER has one to host)
    private val peerConnections = mutableMapOf<String, PeerConnection>()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    // Signaling callbacks — wired by ViewModel
    var onOffer: ((targetId: String, sdp: String) -> Unit)? = null
    var onAnswer: ((targetId: String, sdp: String) -> Unit)? = null
    var onIceCandidate: ((targetId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) -> Unit)? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        Log.d(TAG, "Initialized")
    }

    private fun createPeerConnectionFor(peerId: String): PeerConnection {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val pc = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE state for $peerId: $s")
                }
                override fun onIceConnectionReceivingChange(r: Boolean) {}
                override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        Log.d(TAG, "Local ICE candidate for $peerId")
                        onIceCandidate?.invoke(peerId, it.sdp, it.sdpMid, it.sdpMLineIndex)
                    }
                }
                override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
                override fun onAddStream(s: MediaStream?) {}
                override fun onRemoveStream(s: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track()
                    if (track is VideoTrack) {
                        Log.d(TAG, "Remote video track received from $peerId")
                        _remoteVideoTrack.value = track
                    }
                }
            }
        ) ?: throw IllegalStateException("Failed to create PeerConnection for $peerId")

        peerConnections[peerId] = pc
        return pc
    }

    // HOST: start screen capture
    fun startScreenCapture(mediaProjectionIntent: Intent) {
        Log.d(TAG, "Starting screen capture")
        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCapture", eglBase.eglBaseContext)
        videoSource = peerConnectionFactory.createVideoSource(true)

        screenCapturer = ScreenCapturerAndroid(
            mediaProjectionIntent,
            object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                }
            }
        ).apply {
            initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            startCapture(CAPTURE_WIDTH, CAPTURE_HEIGHT, CAPTURE_FPS)
        }

        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource!!)
        Log.d(TAG, "Screen capture started")
    }

    // HOST: create and send offer to a specific viewer
    fun createOfferFor(viewerId: String) {
        Log.d(TAG, "Creating offer for viewer: $viewerId")
        val pc = createPeerConnectionFor(viewerId)

        localVideoTrack?.let { track ->
            pc.addTrack(track, listOf(STREAM_ID))
        }

        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    pc.setLocalDescription(SimpleSdpObserver(), it)
                    Log.d(TAG, "Offer created for $viewerId")
                    onOffer?.invoke(viewerId, it.description)
                }
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create offer for $viewerId: $error")
            }
        }, MediaConstraints())
    }

    // HOST: process answer received from viewer
    fun processAnswer(viewerId: String, sdp: String) {
        Log.d(TAG, "Processing answer from viewer: $viewerId")
        val pc = peerConnections[viewerId] ?: run {
            Log.w(TAG, "No PeerConnection found for viewer: $viewerId")
            return
        }
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        pc.setRemoteDescription(SimpleSdpObserver(), sessionDescription)
    }

    // VIEWER: process offer from host and auto-create answer
    fun processOffer(hostId: String, sdp: String) {
        Log.d(TAG, "Processing offer from host: $hostId")
        val pc = createPeerConnectionFor(hostId)
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)

        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        answer?.let {
                            pc.setLocalDescription(SimpleSdpObserver(), it)
                            Log.d(TAG, "Answer created for host: $hostId")
                            onAnswer?.invoke(hostId, it.description)
                        }
                    }
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Failed to create answer for $hostId: $error")
                    }
                }, MediaConstraints())
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description from $hostId: $error")
            }
        }, offer)
    }

    // Both: add ICE candidate from a remote peer
    fun addIceCandidate(peerId: String, candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        val pc = peerConnections[peerId] ?: run {
            Log.w(TAG, "No PeerConnection for ICE candidate from: $peerId")
            return
        }
        pc.addIceCandidate(IceCandidate(sdpMid ?: "", sdpMLineIndex ?: 0, candidate))
    }

    fun stopScreenCapture() {
        Log.d(TAG, "Stopping screen capture")
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        screenCapturer = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        videoSource?.dispose()
        videoSource = null
        localVideoTrack?.dispose()
        localVideoTrack = null
    }

    fun release() {
        Log.d(TAG, "Releasing WebRtcManager")
        stopScreenCapture()
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        if (::peerConnectionFactory.isInitialized) {
            peerConnectionFactory.dispose()
        }
        eglBase.release()
        _remoteVideoTrack.value = null
    }
}
