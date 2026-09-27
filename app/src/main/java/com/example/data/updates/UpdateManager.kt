package com.example.data.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GithubRelease(
    val tagName: String,
    val title: String,
    val notes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long
)

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpToDate(val currentVersion: String) : UpdateCheckState()
    data class UpdateAvailable(val release: GithubRelease) : UpdateCheckState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateCheckState()
    data class DownloadComplete(val apkFile: File) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

class UpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val prefs = context.getSharedPreferences("app_updates_pref", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    fun getSavedRepository(): String {
        return prefs.getString("github_repo", "friends-chat/cipher-chat") ?: "friends-chat/cipher-chat"
    }

    fun saveRepository(repo: String) {
        prefs.edit().putString("github_repo", repo.trim()).apply()
    }

    suspend fun checkForUpdates(repo: String = getSavedRepository()) = withContext(Dispatchers.IO) {
        _updateState.value = UpdateCheckState.Checking
        try {
            val cleanRepo = repo.trim().removePrefix("https://github.com/").removePrefix("/")
            if (!cleanRepo.contains("/") || cleanRepo.split("/").size != 2) {
                _updateState.value = UpdateCheckState.Error("Invalid GitHub repository format. Use 'owner/repo'.")
                return@withContext
            }

            val candidateReleases = mutableListOf<JSONObject>()

            // 1. Query /releases first (includes pre-releases and beta tags)
            val urlAll = "https://api.github.com/repos/$cleanRepo/releases"
            val requestAll = Request.Builder()
                .url(urlAll)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "CipherChat-Android")
                .build()

            try {
                okHttpClient.newCall(requestAll).execute().use { response ->
                    if (response.code == 404) {
                        _updateState.value = UpdateCheckState.Error(
                            "Repository '$cleanRepo' was not found or is private on GitHub. Ensure the repository exists and is public."
                        )
                        return@withContext
                    }
                    if (response.isSuccessful && response.body != null) {
                        val array = JSONArray(response.body!!.string())
                        for (i in 0 until array.length()) {
                            candidateReleases.add(array.getJSONObject(i))
                        }
                    }
                }
            } catch (e: Exception) {
                // Proceed to fallback
            }

            // 2. Fallback to /releases/latest if /releases returned nothing
            if (candidateReleases.isEmpty()) {
                val urlLatest = "https://api.github.com/repos/$cleanRepo/releases/latest"
                val requestLatest = Request.Builder()
                    .url(urlLatest)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "CipherChat-Android")
                    .build()

                try {
                    okHttpClient.newCall(requestLatest).execute().use { response ->
                        if (response.isSuccessful && response.body != null) {
                            candidateReleases.add(JSONObject(response.body!!.string()))
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (candidateReleases.isEmpty()) {
                _updateState.value = UpdateCheckState.Error(
                    "No published releases found on 'https://github.com/$cleanRepo'. Create a release at github.com/$cleanRepo/releases/new and attach an .apk file."
                )
                return@withContext
            }

            // 3. Find the candidate release with the highest version tag that has an attached .apk
            var bestReleaseJson: JSONObject? = null
            var bestTag = ""

            for (release in candidateReleases) {
                val assets = release.optJSONArray("assets") ?: continue
                var hasApk = false
                for (i in 0 until assets.length()) {
                    if (assets.getJSONObject(i).optString("name", "").endsWith(".apk", ignoreCase = true)) {
                        hasApk = true
                        break
                    }
                }
                if (!hasApk) continue

                val tag = release.optString("tag_name", "")
                if (bestReleaseJson == null || isVersionNewer(tag, bestTag)) {
                    bestReleaseJson = release
                    bestTag = tag
                }
            }

            if (bestReleaseJson == null) {
                _updateState.value = UpdateCheckState.Error(
                    "Releases found on '$cleanRepo', but none have an .apk file attached. Upload your compiled APK to your release on GitHub."
                )
                return@withContext
            }

            val responseJson = bestReleaseJson

            val tagName = responseJson.optString("tag_name", "")
            val title = responseJson.optString("name", tagName)
            val notes = responseJson.optString("body", "No release notes provided.")
            val assets = responseJson!!.optJSONArray("assets")

            var apkUrl: String? = null
            var apkName = "update.apk"
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrEmpty()) {
                _updateState.value = UpdateCheckState.Error(
                    "Release '$tagName' found on '$cleanRepo', but it has no .apk file attached. Upload your compiled APK to the release on GitHub."
                )
                return@withContext
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isVersionNewer(tagName, currentVersion)

            if (isNewer) {
                _updateState.value = UpdateCheckState.UpdateAvailable(
                    GithubRelease(
                        tagName = tagName,
                        title = title,
                        notes = notes,
                        apkDownloadUrl = apkUrl,
                        apkFileName = apkName,
                        apkSizeBytes = apkSize
                    )
                )
            } else {
                _updateState.value = UpdateCheckState.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            _updateState.value = UpdateCheckState.Error("Failed to check for updates: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun downloadAndInstall(release: GithubRelease) = withContext(Dispatchers.IO) {
        val totalBytes = release.apkSizeBytes
        _updateState.value = UpdateCheckState.Downloading(0, 0L, totalBytes)

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, release.apkFileName)

        try {
            val request = Request.Builder()
                .url(release.apkDownloadUrl)
                .addHeader("User-Agent", "CipherChat-Android")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _updateState.value = UpdateCheckState.Error("Failed to download APK: HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body ?: throw IllegalStateException("Download response body is empty")
                val actualTotal = if (body.contentLength() > 0) body.contentLength() else totalBytes

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            val percent = if (actualTotal > 0) {
                                ((downloaded * 100) / actualTotal).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            _updateState.value = UpdateCheckState.Downloading(percent, downloaded, actualTotal)
                        }
                        output.flush()
                    }
                }
            }

            _updateState.value = UpdateCheckState.DownloadComplete(apkFile)
            launchInstaller(apkFile)
        } catch (e: Exception) {
            _updateState.value = UpdateCheckState.Error("Download failed: ${e.localizedMessage ?: e.message}")
        }
    }

    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    fun launchInstaller(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateCheckState.Error("Unable to launch installer: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun isVersionNewer(releaseTag: String, currentVersion: String): Boolean {
        val cleanTag = releaseTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        val tagParts = cleanTag.split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val currentParts = cleanCurrent.split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

        val maxLen = maxOf(tagParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val t = tagParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (t > c) return true
            if (t < c) return false
        }
        return false
    }
}
