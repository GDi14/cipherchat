package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L
    var isRecording: Boolean = false
        private set

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "audio_recordings").apply { mkdirs() }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            startTimeMillis = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            cleanup()
            false
        }
    }

    fun stopRecording(): Pair<File, Int>? {
        if (!isRecording || mediaRecorder == null) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            val durationSec = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt().coerceAtLeast(1)
            val file = currentFile
            if (file != null && file.exists() && file.length() > 0) {
                Pair(file, durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            null
        }
    }

    fun cancelRecording() {
        cleanup()
    }

    private fun cleanup() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        currentFile?.delete()
        currentFile = null
    }
}

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    fun isPlaying(path: String): Boolean {
        return mediaPlayer?.isPlaying == true && currentPlayingPath == path
    }

    fun play(
        path: String,
        onProgress: (progressFloat: Float, currentSec: Int) -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
            mediaPlayer = player
            currentPlayingPath = path

            val totalDuration = player.duration.coerceAtLeast(1)

            val runnable = object : Runnable {
                override fun run() {
                    val p = mediaPlayer
                    if (p != null && p.isPlaying) {
                        val currentPos = p.currentPosition
                        val fraction = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                        val sec = currentPos / 1000
                        onProgress(fraction, sec)
                        handler.postDelayed(this, 100)
                    }
                }
            }
            progressRunnable = runnable
            handler.post(runnable)

            player.setOnCompletionListener {
                stop()
                onCompletion()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            stop()
            onCompletion()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        progressRunnable?.let { handler.removeCallbacks(it) }
    }

    fun stop() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingPath = null
    }
}
