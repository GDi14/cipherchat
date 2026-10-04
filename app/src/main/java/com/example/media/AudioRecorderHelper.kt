package com.example.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun startRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "audio_records").apply { mkdirs() }
            val outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            return outputFile
        } catch (e: IOException) {
            Log.e("AudioRecorderHelper", "Failed to start recording", e)
            stopRecording()
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recording", e)
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun isRecording(): Boolean = isRecording
}
