package com.example.ui.conversation.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioPlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingMessageId: String? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun playAudio(messageId: String, audioFile: File) {
        if (!audioFile.exists()) {
            Log.e("AudioPlayerManager", "Audio file does not exist: ${audioFile.absolutePath}")
            return
        }

        if (currentPlayingMessageId == messageId && mediaPlayer != null) {
            val mp = mediaPlayer!!
            if (mp.isPlaying) {
                mp.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            } else {
                mp.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
            }
            return
        }

        stopAudio()

        try {
            val mp = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(audioFile))
                prepare()
                setOnCompletionListener {
                    _playbackState.value = AudioPlaybackState()
                    currentPlayingMessageId = null
                    releasePlayer()
                }
            }

            mediaPlayer = mp
            currentPlayingMessageId = messageId
            mp.start()

            _playbackState.value = AudioPlaybackState(
                activeMessageId = messageId,
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = mp.duration
            )
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio file", e)
            stopAudio()
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            currentPlayingMessageId = null
            _playbackState.value = AudioPlaybackState()
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        stopAudio()
    }
}
