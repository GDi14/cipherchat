package com.example.media

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = mediaPlayer
            if (player != null) {
                runCatching {
                    if (player.isPlaying) _positionMs.value = player.currentPosition.toLong()
                    _durationMs.value = player.duration.coerceAtLeast(0).toLong()
                }
                if (_isPlaying.value) handler.postDelayed(this, 100L)
            }
        }
    }
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playAudio(pathOrUrl: String, onCompletion: () -> Unit = {}) {
        if (_currentPlayingPath.value == pathOrUrl && mediaPlayer != null) {
            resumeAudio()
            return
        }

        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                if (pathOrUrl.startsWith("http")) {
                    setDataSource(pathOrUrl)
                } else {
                    val file = File(pathOrUrl)
                    if (!file.exists()) {
                        Log.e("AudioPlayerHelper", "Audio file does not exist: $pathOrUrl")
                        return
                    }
                    setDataSource(file.absolutePath)
                }
                setOnPreparedListener {
                    _durationMs.value = it.duration.coerceAtLeast(0).toLong()
                    it.playbackParams = it.playbackParams.setSpeed(_playbackSpeed.value)
                    start()
                    _isPlaying.value = true
                    _currentPlayingPath.value = pathOrUrl
                    handler.removeCallbacks(progressUpdater)
                    handler.post(progressUpdater)
                }
                setOnCompletionListener {
                    handler.removeCallbacks(progressUpdater)
                    _isPlaying.value = false
                    _positionMs.value = _durationMs.value
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerHelper", "Playback failed: $what/$extra")
                    stopAudio()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error playing audio", e)
            stopAudio()
        }
    }

    fun pauseAudio() {
        runCatching {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
            _positionMs.value = mediaPlayer?.currentPosition?.toLong() ?: _positionMs.value
        }
        _isPlaying.value = false
        handler.removeCallbacks(progressUpdater)
    }

    fun resumeAudio() {
        runCatching {
            val player = mediaPlayer ?: return
            if (player.currentPosition >= player.duration && player.duration > 0) player.seekTo(0)
            player.start()
            _isPlaying.value = true
            handler.removeCallbacks(progressUpdater)
            handler.post(progressUpdater)
        }.onFailure { Log.e("AudioPlayerHelper", "Unable to resume audio", it) }
    }

    fun seekTo(positionMs: Long) {
        runCatching {
            val player = mediaPlayer ?: return
            val boundedPosition = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0).toLong())
            player.seekTo(boundedPosition.toInt())
            _positionMs.value = boundedPosition
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        runCatching {
            _playbackSpeed.value = speed
            val player = mediaPlayer ?: return
            player.playbackParams = player.playbackParams.setSpeed(speed)
        }
            .onFailure { Log.w("AudioPlayerHelper", "Playback speed is unavailable", it) }
    }

    fun stopAudio() {
        handler.removeCallbacks(progressUpdater)
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerHelper", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingPath.value = null
            _positionMs.value = 0L
            _durationMs.value = 0L
        }
    }
}
