package com.example.media

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.crypto.MediaEncryptor
import okio.buffer
import okio.source
import java.io.File

data class EncryptedMediaRequest(
    val file: File,
    val keyBase64: String,
    val ivBase64: String,
    val mimeType: String? = null
)

/**
 * Custom Coil Fetcher that streams and decrypts AES-256-GCM encrypted media on-the-fly directly into Bitmaps,
 * eliminating plaintext disk residue.
 */
class EncryptedMediaFetcher(
    private val data: EncryptedMediaRequest,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): SourceResult {
        val cipherInputStream = MediaEncryptor.decryptToStream(
            encryptedFile = data.file,
            keyBase64 = data.keyBase64,
            ivBase64 = data.ivBase64
        )

        val imageSource = ImageSource(
            source = cipherInputStream.source().buffer(),
            context = options.context
        )

        return SourceResult(
            source = imageSource,
            mimeType = data.mimeType,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<EncryptedMediaRequest> {
        override fun create(
            data: EncryptedMediaRequest,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return EncryptedMediaFetcher(data, options)
        }
    }
}
