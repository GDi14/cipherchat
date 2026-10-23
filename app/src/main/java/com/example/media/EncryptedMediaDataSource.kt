package com.example.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.example.crypto.MediaEncryptor
import java.io.File
import java.io.InputStream

/**
 * Custom Media3 ExoPlayer DataSource that streams and decrypts AES-256-GCM audio/video
 * files directly via [InputStream] without writing plaintext bytes to disk.
 */
@OptIn(UnstableApi::class)
class EncryptedMediaDataSource(
    private val encryptedFile: File,
    private val keyBase64: String,
    private val ivBase64: String
) : BaseDataSource(/* isNetwork = */ false) {

    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        val stream = MediaEncryptor.decryptToStream(encryptedFile, keyBase64, ivBase64)
        inputStream = stream

        if (dataSpec.position > 0) {
            stream.skip(dataSpec.position)
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val totalLength = encryptedFile.length() - 16 // Account for GCM 16-byte auth tag
            if (totalLength > dataSpec.position) totalLength - dataSpec.position else C.LENGTH_UNSET.toLong()
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val stream = inputStream ?: return C.RESULT_END_OF_INPUT
        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }

        val bytesRead = stream.read(buffer, offset, bytesToRead)
        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            inputStream?.close()
        } finally {
            inputStream = null
            transferEnded()
        }
    }

    class Factory(
        private val encryptedFile: File,
        private val keyBase64: String,
        private val ivBase64: String
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return EncryptedMediaDataSource(encryptedFile, keyBase64, ivBase64)
        }
    }
}
