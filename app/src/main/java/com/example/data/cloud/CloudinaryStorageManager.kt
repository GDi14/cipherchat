package com.example.data.cloud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class CloudinaryStorageManager(context: Context) {

    companion object {
        // Hardcoded Cloudinary Server Credentials
        const val DEFAULT_CLOUD_NAME = "wha-cipher"
        const val DEFAULT_API_KEY = "563781351987439"
        const val DEFAULT_API_SECRET = "zfXmpnXtpZFLbmAYdoTvcK7FTvc"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("cloudinary_config", Context.MODE_PRIVATE)

    var cloudName: String
        get() {
            val saved = prefs.getString("cloud_name", null)
            return if (!saved.isNullByBlank()) saved!! else DEFAULT_CLOUD_NAME
        }
        set(value) { prefs.edit().putString("cloud_name", value.trim()).apply() }

    var apiKey: String
        get() {
            val saved = prefs.getString("api_key", null)
            return if (!saved.isNullByBlank()) saved!! else DEFAULT_API_KEY
        }
        set(value) { prefs.edit().putString("api_key", value.trim()).apply() }

    var apiSecret: String
        get() {
            val saved = prefs.getString("api_secret", null)
            return if (!saved.isNullByBlank()) saved!! else DEFAULT_API_SECRET
        }
        set(value) { prefs.edit().putString("api_secret", value.trim()).apply() }

    fun isConfigured(): Boolean {
        return cloudName.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank()
    }

    fun setCredentials(name: String, key: String, secret: String) {
        this.cloudName = name
        this.apiKey = key
        this.apiSecret = secret
    }

    suspend fun uploadEncryptedAttachment(
        encryptedBytes: ByteArray,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Cloudinary is not configured yet."))
        }

        try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signatureStr = "timestamp=$timestamp$apiSecret"
            val signature = sha1Hex(signatureStr)

            val safeRawFileName = "raw_attachment_${UUID.randomUUID()}.dat"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart(
                    "file",
                    safeRawFileName,
                    encryptedBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/raw/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("CloudinaryStorage", "Upload failed: $bodyStr")
                    val parsedError = try {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                    } catch (_: Exception) { null }
                    val errorMsg = parsedError ?: if (bodyStr.isNotBlank()) bodyStr else "HTTP ${response.code} ${response.message}"
                    return@withContext Result.failure(IllegalStateException("Cloudinary: $errorMsg"))
                }

                val json = JSONObject(bodyStr)
                val secureUrl = json.optString("secure_url")
                if (secureUrl.isNotBlank()) {
                    Result.success(secureUrl)
                } else {
                    Result.failure(IllegalStateException("Cloudinary response missing secure_url"))
                }
            }
        } catch (e: Exception) {
            Log.e("CloudinaryStorage", "Exception uploading to Cloudinary", e)
            Result.failure(e)
        }
    }

    suspend fun downloadEncryptedAttachment(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Download failed with code ${response.code}"))
                }
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    Result.success(bytes)
                } else {
                    Result.failure(IllegalStateException("Downloaded empty file"))
                }
            }
        } catch (e: Exception) {
            Log.e("CloudinaryStorage", "Failed to download attachment from $url", e)
            Result.failure(e)
        }
    }

    private fun sha1Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String?.isNullByBlank(): Boolean {
        return this == null || this.isBlank()
    }
}
