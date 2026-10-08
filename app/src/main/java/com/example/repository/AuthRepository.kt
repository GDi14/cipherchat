package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.PrekeyDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.PushTokenRegistrar
import com.example.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import android.util.Log

class AuthRepository(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager,
    private val signalManager: SignalProtocolManager,
    private val supabaseProvider: SupabaseClientProvider,
    private val database: AppDatabase
) {
    val myUserId: String get() = securePrefs.getOrCreateUserId()
    val myDeviceId: String = securePrefs.getOrCreateDeviceId()

    fun isRegistered(): Boolean = securePrefs.isRegistered() && !securePrefs.getSupabaseAuthUserId().isNullOrBlank()
    fun getDisplayName(): String = securePrefs.getDisplayName() ?: "User"
    fun getTag(): String = securePrefs.getTag() ?: ""
    fun getAvatarPath(): String? = securePrefs.getAvatarPath()
    fun getFavoriteFriendIds(): Set<String> = securePrefs.getFavoriteFriendIds()
    fun saveFavoriteFriendIds(ids: Set<String>) = securePrefs.saveFavoriteFriendIds(ids)
    fun isVoiceOfUserEnabled(): Boolean = securePrefs.isVoiceOfUserEnabled()
    fun setVoiceOfUserEnabled(enabled: Boolean) = securePrefs.setVoiceOfUserEnabled(enabled)

    /** Exchange the native Google ID token for a Supabase Auth session and restore a linked profile. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseProvider.getClient()
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            val authUserId = client.auth.currentUserOrNull()?.id
                ?: error("Supabase did not return an authenticated user")
            securePrefs.saveSupabaseAuthUserId(authUserId)

            val linkedProfile = client.postgrest["profiles"].select {
                filter { eq("auth_user_id", authUserId) }
            }.decodeList<ProfileDto>().firstOrNull()

            if (linkedProfile != null) {
                securePrefs.setCurrentUserId(linkedProfile.id)
                securePrefs.saveProfile(
                    linkedProfile.displayName,
                    linkedProfile.tag,
                    linkedProfile.avatarPath,
                    authUserId
                )
                database.profileDao().insertOrUpdate(
                    ProfileEntity(
                        id = linkedProfile.id,
                        displayName = linkedProfile.displayName,
                        tag = linkedProfile.tag,
                        avatarPath = linkedProfile.avatarPath,
                        isFriend = false
                    )
                )
                runCatching { PushTokenRegistrar.register(context, securePrefs, supabaseProvider) }
                Result.success(true)
            } else {
                if (securePrefs.isRegistered()) {
                    securePrefs.clearSupabaseAuthUserId()
                    return@withContext Result.failure(
                        IllegalStateException(
                            "This profile was created before Google sign-in. It must be linked by an administrator before it can be used with the new account security rules."
                        )
                    )
                }
                Result.success(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

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
            val authUserId = securePrefs.getSupabaseAuthUserId()
                ?: return@withContext Result.failure(IllegalStateException("Sign in with Google before creating your profile"))
            val cleanTag = tag.trim().removePrefix("@").lowercase()

            // Storage RLS resolves the active Auth identity through profiles.auth_user_id.
            // Create that link before uploading the avatar, then update the row with its URL below.
            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                check(client.auth.currentUserOrNull()?.id == authUserId) {
                    "Your secure cloud session expired. Sign in again before creating your profile."
                }
                client.postgrest["profiles"].upsert(
                    ProfileDto(
                        id = myUserId,
                        deviceId = myDeviceId,
                        displayName = displayName.trim(),
                        tag = cleanTag,
                        avatarPath = null,
                        authUserId = authUserId
                    )
                )
            }

            // 1. Upload avatar if selected
            var avatarUrl: String? = null
            if (avatarUri != null) {
                check(supabaseProvider.isConfigured) { "Connect to Supabase before uploading a profile photo" }
                avatarUrl = uploadAvatar(avatarUri)
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
                    avatarPath = avatarUrl,
                    authUserId = authUserId
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
            securePrefs.saveProfile(displayName.trim(), cleanTag, avatarUrl, authUserId)
            database.profileDao().insertOrUpdate(
                ProfileEntity(
                    id = myUserId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl,
                    isFriend = false
                )
            )
            runCatching { PushTokenRegistrar.register(context, securePrefs, supabaseProvider) }

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
            if (avatarUri != null) {
                check(supabaseProvider.isConfigured) { "Connect to Supabase before uploading a profile photo" }
                avatarUrl = uploadAvatar(avatarUri)
            }

            if (supabaseProvider.isConfigured) {
                val client = supabaseProvider.getClient()
                val profileDto = ProfileDto(
                    id = myUserId,
                    deviceId = myDeviceId,
                    displayName = displayName.trim(),
                    tag = cleanTag,
                    avatarPath = avatarUrl,
                    authUserId = securePrefs.getSupabaseAuthUserId()
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

    private suspend fun uploadAvatar(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read the selected profile photo")
        require(bytes.isNotEmpty()) { "The selected profile photo is empty" }
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "${myUserId}_${System.currentTimeMillis()}.$extension"
        val bucket = supabaseProvider.getClient().storage["avatars"]
        try {
            bucket.upload(fileName, bytes) { upsert = true }
            return bucket.publicUrl(fileName)
        } catch (error: Exception) {
            Log.e("AuthRepository", "Avatar upload failed for profile $myUserId", error)
            throw error
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
                avatarPath = avatarUrl,
                authUserId = securePrefs.getSupabaseAuthUserId()
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
