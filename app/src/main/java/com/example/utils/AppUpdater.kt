package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val GITHUB_API_URL = "https://api.github.com/repos/GDI14/cipherchat/releases?per_page=20"

    data class ReleaseInfo(
        val tagName: String,
        val apkUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdate(context: Context): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Failed to check update: HTTP ${connection.responseCode}")
                return@withContext null
            }

            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val releases = org.json.JSONArray(responseString)
            for (releaseIndex in 0 until releases.length()) {
                val json = releases.optJSONObject(releaseIndex) ?: continue
                if (json.optBoolean("draft", true)) continue
                val tagName = json.optString("tag_name", "")
                val body = json.optString("body", "")
                val assets = json.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                if (tagName.isNotBlank() && apkUrl.isNotBlank()) {
                    return@withContext ReleaseInfo(tagName = tagName, apkUrl = apkUrl, releaseNotes = body)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            null
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "cipher_update.apk")
            if (apkFile.exists()) apkFile.delete()

            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                connect()
            }

            val fileLength = connection.contentLength
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val data = ByteArray(16 * 1024)
                    var total = 0L
                    var lastProgress = -1
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count.toLong()
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (apkFile.length() == 0L || (fileLength > 0 && apkFile.length() != fileLength.toLong())) {
                apkFile.delete()
                throw IllegalStateException("The downloaded APK is incomplete")
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
                installApk(context, apkFile)
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Download failed")
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
        }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun isNewerRelease(releaseTag: String, currentVersion: String): Boolean {
        fun parts(value: String): List<Int> = value.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-').substringBefore('+')
            .split('.')
            .mapNotNull { token -> token.takeWhile(Char::isDigit).toIntOrNull() }

        val latest = parts(releaseTag)
        val installed = parts(currentVersion)
        if (latest.isEmpty() || installed.isEmpty()) return false
        for (index in 0 until maxOf(latest.size, installed.size)) {
            val latestPart = latest.getOrElse(index) { 0 }
            val installedPart = installed.getOrElse(index) { 0 }
            if (latestPart != installedPart) return latestPart > installedPart
        }
        val latestIsPrerelease = '-' in releaseTag
        val installedIsPrerelease = '-' in currentVersion
        if (latestIsPrerelease != installedIsPrerelease) return installedIsPrerelease && !latestIsPrerelease
        if (latestIsPrerelease) {
            val latestSuffix = releaseTag.substringAfter('-', "").split('.')
            val installedSuffix = currentVersion.substringAfter('-', "").split('.')
            for (index in 0 until maxOf(latestSuffix.size, installedSuffix.size)) {
                val latestPart = latestSuffix.getOrElse(index) { "" }
                val installedPart = installedSuffix.getOrElse(index) { "" }
                if (latestPart == installedPart) continue
                val latestNumber = latestPart.toLongOrNull()
                val installedNumber = installedPart.toLongOrNull()
                if (latestNumber != null && installedNumber != null) return latestNumber > installedNumber
                if (latestNumber != null) return false // Numeric identifiers sort before text identifiers.
                if (installedNumber != null) return true
                return latestPart.compareTo(installedPart, ignoreCase = true) > 0
            }
            return false
        }
        return false
    }
}
