package com.example.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var amplitudeJob: Job? = null

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds

    fun startRecording(coroutineScope: CoroutineScope): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "audio").apply { mkdirs() }
            val outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            recordingStartTime = System.currentTimeMillis()
            _amplitudes.value = emptyList()
            _durationSeconds.value = 0

            amplitudeJob = coroutineScope.launch(Dispatchers.Default) {
                val sampleList = mutableListOf<Float>()
                while (isActive) {
                    delay(100)
                    val maxAmp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    sampleList.add(normalized)
                    if (sampleList.size > 40) sampleList.removeAt(0)
                    _amplitudes.value = sampleList.toList()
                    _durationSeconds.value = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            false
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        amplitudeJob?.cancel()
        amplitudeJob = null
        val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
        val file = currentOutputFile

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
        return Pair(file, duration)
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            recorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }
}

class VoicePlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    fun play(filePath: String, coroutineScope: CoroutineScope, onFinished: () -> Unit = {}) {
        if (_currentPlayingPath.value == filePath && _isPlaying.value) {
            pause()
            return
        }

        stop()

        try {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            mediaPlayer = mp
            _isPlaying.value = true
            _currentPlayingPath.value = filePath

            mp.setOnCompletionListener {
                _isPlaying.value = false
                _currentPosition.value = 0f
                _currentPlayingPath.value = null
                progressJob?.cancel()
                onFinished()
            }

            progressJob = coroutineScope.launch(Dispatchers.Default) {
                while (isActive && mp.isPlaying) {
                    val duration = mp.duration.coerceAtLeast(1)
                    val pos = mp.currentPosition
                    _currentPosition.value = (pos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    delay(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0f
            _currentPlayingPath.value = null
        }
    }
}

object MediaDownloader {
    fun saveToDownloads(context: Context, sourceFile: File, displayName: String, mimeType: String): Result<Uri> {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Cipher")
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Failed to insert MediaStore record"))

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(out)
                }
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
