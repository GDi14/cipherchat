package com.example.ui.conversation.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class VoiceRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun startRecording(): Boolean {
        try {
            stopRecording() // ensure previous clean state
            val outputDir = context.cacheDir
            currentOutputFile = File.createTempFile("voice_memo_", ".m4a", outputDir)

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
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(currentOutputFile?.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            return true
        } catch (e: IOException) {
            Log.e("VoiceRecorderManager", "Failed to start recording", e)
            cleanup()
            return false
        } catch (e: IllegalStateException) {
            Log.e("VoiceRecorderManager", "IllegalStateException starting recording", e)
            cleanup()
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentOutputFile
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Failed to stop recording cleanly", e)
            cleanup()
            null
        }
    }

    fun cancelRecording() {
        stopRecording()
        currentOutputFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        currentOutputFile = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
    }
}
