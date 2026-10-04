package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object CloudinaryUploader {
    const val CLOUD_NAME = "tgw3iao5"
    // Unsigned preset created by user. Literal placeholder as instructed by prompt:
    const val UPLOAD_PRESET = "[PRESET_NAME]"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/auto/upload"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadEncryptedBytes(
        encryptedBytes: ByteArray,
        filename: String = "encrypted_payload.bin"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/octet-stream".toMediaType()
            val fileBody = encryptedBytes.toRequestBody(mediaType)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("file", filename, fileBody)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Cloudinary upload failed (HTTP ${response.code}): $bodyString")
                )
            }

            val json = JSONObject(bodyString)
            val secureUrl = json.optString("secure_url", "")
            if (secureUrl.isNotEmpty()) {
                Result.success(secureUrl)
            } else {
                Result.failure(IOException("Cloudinary response missing secure_url: $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadEncryptedBytes(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to download media: HTTP ${response.code}"))
            }
            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(IOException("Empty response body"))
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
