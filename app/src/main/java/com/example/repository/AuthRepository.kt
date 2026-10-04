package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.PrekeyDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class AuthRepository(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager,
    private val signalManager: SignalProtocolManager,
    private val supabaseProvider: SupabaseClientProvider,
    private val database: AppDatabase
) {
    val myUserId: String = securePrefs.getOrCreateUserId()
    val myDeviceId: String = securePrefs.getOrCreateDeviceId()

    fun isRegistered(): Boolean = securePrefs.isRegistered()
    fun getDisplayName(): String = securePrefs.getDisplayName() ?: "User"
    fun getTag(): String = securePrefs.getTag() ?: ""
    fun getAvatarPath(): String? = securePrefs.getAvatarPath()

    /**
     * Checks if a tag is available on Supabase.
     */
    suspend fun checkTagAvailable(tag: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = tag.trim().removePrefix("@").lowercase()
            if (cleanTag.isBlank()) return@withContext Result.success(false)

            if (!supabaseProvider.isConfigured) {
                // If Supabase not connected yet, assume available locally
                return@withContext Result.success(true)
            }

            val client = supabaseProvider.getClient()
            val existing = client.postgrest["profiles"].select {
                filter {
                    eq("tag", cleanTag)
                }
            }.decodeList<ProfileDto>()

            // Available if empty, or if belongs to current user
            val available = existing.isEmpty() || existing.any { it.id == myUserId }
            Result.success(available)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fail safely with error
            Result.failure(e)
        }
    }

    /**
     * Registers local device identity with Supabase.
     * Generates libsignal identity and prekeys locally, stores private keys in Keystore,
     * and uploads only public identity and prekey material.
     */
    suspend fun registerUser(displayName: String, tag: String, avatarUri: Uri? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = tag.trim().removePrefix("@").lowercase()

            // 1. Upload avatar if selected
            var avatarUrl: String? = null
            if (avatarUri != null && supabaseProvider.isConfigured) {
                try {
                    val bytes = context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val client = supabaseProvider.getClient()
                        val fileName = "${myUserId}_${System.currentTimeMillis()}.jpg"
                        val bucket = client.storage["avatars"]
                        bucket.upload(fileName, bytes) {
                            upsert = true
                        }
                        avatarUrl = bucket.publicUrl(fileName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Generate public prekey bundle
            val bundle = signalManager.generatePublicPrekeyBundle()

            // 3. Upsert profiles and prekeys on Supabase if connected
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val profileDto = ProfileDto(
                    id = myUserId,
                    deviceId = myDeviceId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl
                )
                client.postgrest["profiles"].upsert(profileDto)

                val prekeyDto = PrekeyDto(
                    userId = myUserId,
                    identityKey = bundle.identityKey,
                    registrationId = bundle.registrationId,
                    signedPrekeyId = bundle.signedPrekeyId,
                    signedPrekey = bundle.signedPrekey,
                    signedPrekeySignature = bundle.signedPrekeySignature,
                    oneTimePrekeys = bundle.oneTimePrekeys
                )
                client.postgrest["prekeys"].upsert(prekeyDto)
            }

            // 4. Save profile in SecurePreferences and Room
            securePrefs.saveProfile(displayName.trim(), cleanTag, avatarUrl)
            database.profileDao().insertOrUpdate(
                ProfileEntity(
                    id = myUserId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl,
                    isFriend = false
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Updates own profile details.
     */
    suspend fun updateProfile(displayName: String, tag: String, avatarUri: Uri? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanTag = tag.trim().removePrefix("@").lowercase()

            var avatarUrl: String? = securePrefs.getAvatarPath()
            if (avatarUri != null && supabaseProvider.isConfigured) {
                try {
                    val bytes = context.contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val client = supabaseProvider.getClient()
                        val fileName = "${myUserId}_${System.currentTimeMillis()}.jpg"
                        val bucket = client.storage["avatars"]
                        bucket.upload(fileName, bytes) {
                            upsert = true
                        }
                        avatarUrl = bucket.publicUrl(fileName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val profileDto = ProfileDto(
                    id = myUserId,
                    deviceId = myDeviceId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl
                )
                client.postgrest["profiles"].upsert(profileDto)
            }

            securePrefs.saveProfile(displayName.trim(), cleanTag, avatarUrl)
            database.profileDao().insertOrUpdate(
                ProfileEntity(
                    id = myUserId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl,
                    isFriend = false
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Publishes the user's profile and libsignal public prekey bundle to Supabase.
     * Can be invoked at app launch or when Supabase credentials are updated in Settings.
     */
    suspend fun syncProfileToSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!supabaseProvider.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
            }

            val displayName = securePrefs.getDisplayName() ?: return@withContext Result.success(Unit)
            val tag = securePrefs.getTag() ?: return@withContext Result.success(Unit)
            val avatarUrl = securePrefs.getAvatarPath()

            val bundle = signalManager.generatePublicPrekeyBundle()

            val client = supabaseProvider.getClient()
            val profileDto = ProfileDto(
                id = myUserId,
                deviceId = myDeviceId,
                displayName = displayName,
                tag = tag,
                avatarPath = avatarUrl
            )
            client.postgrest["profiles"].upsert(profileDto)

            val prekeyDto = PrekeyDto(
                userId = myUserId,
                identityKey = bundle.identityKey,
                registrationId = bundle.registrationId,
                signedPrekeyId = bundle.signedPrekeyId,
                signedPrekey = bundle.signedPrekey,
                signedPrekeySignature = bundle.signedPrekeySignature,
                oneTimePrekeys = bundle.oneTimePrekeys
            )
            client.postgrest["prekeys"].upsert(prekeyDto)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
