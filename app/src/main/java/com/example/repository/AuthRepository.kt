package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileEntity
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.PrekeyDto
import com.example.data.remote.ProfileDto
import com.example.data.remote.ProfileAppearance
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
import android.util.Base64

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
    fun profileAppearance(): ProfileAppearance = ProfileAppearance(
        background = securePrefs.getProfileBackgroundColor(), fontId = securePrefs.getProfileFontId(),
        width = securePrefs.getProfileFontWidth(), grade = securePrefs.getProfileFontGrade(),
        weight = securePrefs.getProfileFontWeight(), opticalSize = securePrefs.getProfileOpticalSize(),
        bio = securePrefs.getProfileBio()
    )

    fun storeProfileAppearance(appearance: ProfileAppearance) {
        securePrefs.setProfileBackgroundColor(appearance.background)
        securePrefs.setProfileFontId(appearance.fontId)
        securePrefs.setProfileFontWidth(appearance.width)
        securePrefs.setProfileFontGrade(appearance.grade)
        securePrefs.setProfileFontWeight(appearance.weight)
        securePrefs.setProfileOpticalSize(appearance.opticalSize)
        securePrefs.setProfileBio(appearance.bio)
    }

    suspend fun saveProfileAppearance(appearance: ProfileAppearance): Result<Unit> = withContext(Dispatchers.IO) {
        storeProfileAppearance(appearance)
        if (!supabaseProvider.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Connect to Supabase to share your profile appearance with contacts"))
        }
        runCatching {
            val client = supabaseProvider.getClient()
            val authUserId = securePrefs.getSupabaseAuthUserId()
                ?: error("Sign in to Supabase before publishing your profile appearance")
            check(client.auth.currentUserOrNull()?.id == authUserId) {
                "Your secure cloud session expired. Sign in again before publishing profile changes."
            }
            // Resolve the canonical profile row by Auth identity first. On devices that
            // re-linked an account, the local device UUID may differ from the public row.
            val existingProfile = client.postgrest["profiles"].select {
                filter { eq("auth_user_id", authUserId) }
            }.decodeList<ProfileDto>().firstOrNull() ?: client.postgrest["profiles"].select {
                filter { eq("id", myUserId) }
            }.decodeList<ProfileDto>().firstOrNull()
            val profileId = existingProfile?.id ?: myUserId
            val profileDeviceId = existingProfile?.deviceId ?: myDeviceId
            client.postgrest["profiles"].upsert(ProfileDto(
                id = profileId, deviceId = profileDeviceId,
                displayName = getDisplayName(), tag = getTag(), avatarPath = getAvatarPath(),
                authUserId = authUserId,
                profileBackground = appearance.background, profileFont = appearance.fontId,
                profileFontWidth = appearance.width, profileFontGrade = appearance.grade,
                profileFontWeight = appearance.weight, profileFontOpticalSize = appearance.opticalSize,
                profileBio = appearance.bio
            ))
            val publishedProfile = client.postgrest["profiles"].select {
                filter { eq("id", profileId) }
            }.decodeList<ProfileDto>().firstOrNull()
                ?: error("Supabase did not return the profile after saving its appearance")
            check(
                publishedProfile.profileFont == appearance.fontId &&
                    publishedProfile.profileFontWeight == appearance.weight &&
                    kotlin.math.abs(publishedProfile.profileFontWidth - appearance.width) < 0.01f
            ) {
                "Supabase did not save the selected profile font, weight, and width. Check the profile columns and update policy in supabase_schema.sql."
            }
            if (profileId != myUserId) securePrefs.setCurrentUserId(profileId)
        }.map { Unit }.map { Unit }
    }

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
                securePrefs.setProfileBackgroundColor(linkedProfile.profileBackground)
                securePrefs.setProfileFontId(linkedProfile.profileFont)
                securePrefs.setProfileFontWidth(linkedProfile.profileFontWidth)
                securePrefs.setProfileFontGrade(linkedProfile.profileFontGrade)
                securePrefs.setProfileFontWeight(linkedProfile.profileFontWeight)
                securePrefs.setProfileOpticalSize(linkedProfile.profileFontOpticalSize)
                securePrefs.setProfileBio(linkedProfile.profileBio)
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
                    .onFailure { Log.e("PushTokenRegistrar", "FCM token registration failed after sign-in", it) }
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
                        authUserId = authUserId,
                        profileBackground = profileAppearance().background,
                        profileFont = profileAppearance().fontId,
                        profileFontWidth = profileAppearance().width,
                        profileFontGrade = profileAppearance().grade,
                        profileFontWeight = profileAppearance().weight,
                        profileFontOpticalSize = profileAppearance().opticalSize,
                        profileBio = profileAppearance().bio
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
                    authUserId = authUserId,
                    profileBackground = profileAppearance().background,
                    profileFont = profileAppearance().fontId,
                    profileFontWidth = profileAppearance().width,
                    profileFontGrade = profileAppearance().grade,
                    profileFontWeight = profileAppearance().weight,
                    profileFontOpticalSize = profileAppearance().opticalSize,
                    profileBio = profileAppearance().bio
                )
                client.postgrest["profiles"].upsert(profileDto)

                val prekeyDto = PrekeyDto(
                    userId = myUserId,
                    deviceId = myDeviceId,
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
                .onFailure { Log.e("PushTokenRegistrar", "FCM token registration failed after profile setup", it) }
                .onFailure { Log.e("PushTokenRegistrar", "FCM token registration failed after profile setup", it) }

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
                    authUserId = securePrefs.getSupabaseAuthUserId(),
                    profileBackground = profileAppearance().background,
                    profileFont = profileAppearance().fontId,
                    profileFontWidth = profileAppearance().width,
                    profileFontGrade = profileAppearance().grade,
                    profileFontWeight = profileAppearance().weight,
                    profileFontOpticalSize = profileAppearance().opticalSize,
                    profileBio = profileAppearance().bio
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

    /** Publishes the profile and ensures public Signal keys exist without rotating them on launch. */
    suspend fun syncProfileToSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!supabaseProvider.isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
            }

            val displayName = securePrefs.getDisplayName() ?: return@withContext Result.success(Unit)
            val tag = securePrefs.getTag() ?: return@withContext Result.success(Unit)
            val avatarUrl = securePrefs.getAvatarPath()

            val client = supabaseProvider.getClient()
            val profileDto = ProfileDto(
                id = myUserId,
                deviceId = myDeviceId,
                displayName = displayName,
                tag = tag,
                avatarPath = avatarUrl,
                authUserId = securePrefs.getSupabaseAuthUserId(),
                profileBackground = profileAppearance().background,
                profileFont = profileAppearance().fontId,
                profileFontWidth = profileAppearance().width,
                profileFontGrade = profileAppearance().grade,
                profileFontWeight = profileAppearance().weight,
                profileFontOpticalSize = profileAppearance().opticalSize,
                profileBio = profileAppearance().bio
            )
            client.postgrest["profiles"].upsert(profileDto)

            // Do not rotate keys on routine launches. Rotate when the account has moved to a
            // different local device or when its published identity no longer matches the private
            // identity persisted locally (for example after reinstall or Keystore recovery).
            val existingKeys = client.postgrest["prekeys"].select {
                filter { eq("user_id", myUserId) }
            }.decodeList<PrekeyDto>().firstOrNull()
            val localIdentityKey = Base64.encodeToString(
                signalManager.localIdentityKeyPair.publicKey.serialize(),
                Base64.NO_WRAP
            )
            if (existingKeys == null ||
                existingKeys.deviceId != myDeviceId ||
                existingKeys.identityKey != localIdentityKey
            ) {
                val bundle = signalManager.generatePublicPrekeyBundle()
                client.postgrest["prekeys"].upsert(
                    PrekeyDto(
                        userId = myUserId,
                        deviceId = myDeviceId,
                        identityKey = bundle.identityKey,
                        registrationId = bundle.registrationId,
                        signedPrekeyId = bundle.signedPrekeyId,
                        signedPrekey = bundle.signedPrekey,
                        signedPrekeySignature = bundle.signedPrekeySignature,
                        oneTimePrekeys = bundle.oneTimePrekeys
                    )
                )
            } else if (existingKeys.oneTimePrekeys.size < 5) {
                val freshBundle = signalManager.generatePublicPrekeyBundle()
                client.postgrest["prekeys"].upsert(
                    existingKeys.copy(
                        oneTimePrekeys = (existingKeys.oneTimePrekeys + freshBundle.oneTimePrekeys).distinctBy { it.id }
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
