package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import java.util.UUID

/**
 * Manages device identity and authentication state securely using Android KeyStore-backed
 * EncryptedSharedPreferences.
 */
class SecurePreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    val encryptedPrefs: SharedPreferences = openEncryptedPreferences()

    private fun openEncryptedPreferences(): SharedPreferences {
        try {
            return createEncryptedPreferences()
        } catch (error: Exception) {
            if (!error.hasAeadBadTag()) throw error

            // A restored preferences file can outlive the Android Keystore key that encrypted it.
            // Those values (including local Signal private keys) cannot be recovered; remove only
            // this file and its unusable wrapping key, then allow the app to start with fresh keys.
            appContext.deleteSharedPreferences(PREFS_FILE)
            runCatching {
                KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                    load(null)
                    if (containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                        deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    }
                }
            }.getOrThrow()

            return createEncryptedPreferences()
        }
    }

    private fun createEncryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun Throwable.hasAeadBadTag(): Boolean =
        generateSequence(this) { it.cause }.any { it is AEADBadTagException }

    fun getOrCreateUserId(): String {
        var userId = encryptedPrefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrBlank()) {
            userId = UUID.randomUUID().toString()
            encryptedPrefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    fun getOrCreateDeviceId(): String {
        var deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            encryptedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun saveProfile(displayName: String, tag: String, avatarPath: String?, authUserId: String? = getSupabaseAuthUserId()) {
        encryptedPrefs.edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_TAG, tag)
            .putString(KEY_AVATAR_PATH, avatarPath)
            .putBoolean(KEY_REGISTERED, true)
            .putString(KEY_SUPABASE_AUTH_USER_ID, authUserId)
            .apply()
    }

    fun setCurrentUserId(userId: String) {
        encryptedPrefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getSupabaseAuthUserId(): String? = encryptedPrefs.getString(KEY_SUPABASE_AUTH_USER_ID, null)

    fun saveSupabaseAuthUserId(userId: String) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_AUTH_USER_ID, userId).apply()
    }

    fun clearSupabaseAuthUserId() {
        encryptedPrefs.edit().remove(KEY_SUPABASE_AUTH_USER_ID).apply()
    }

    fun getDisplayName(): String? = encryptedPrefs.getString(KEY_DISPLAY_NAME, null)
    fun getTag(): String? = encryptedPrefs.getString(KEY_TAG, null)
    fun getAvatarPath(): String? = encryptedPrefs.getString(KEY_AVATAR_PATH, null)
    fun isRegistered(): Boolean = encryptedPrefs.getBoolean(KEY_REGISTERED, false)
    fun isVoiceOfUserEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_VOICE_OF_USER, true)
    fun setVoiceOfUserEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_VOICE_OF_USER, enabled).apply()
    }

    fun isEdgeDockEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_EDGE_DOCK_ENABLED, false)
    fun setEdgeDockEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_EDGE_DOCK_ENABLED, enabled).apply()
    }

    fun getProfileBackgroundColor(): String = encryptedPrefs.getString(KEY_PROFILE_BACKGROUND, "#C987FA") ?: "#C987FA"
    fun setProfileBackgroundColor(value: String) { encryptedPrefs.edit().putString(KEY_PROFILE_BACKGROUND, value).apply() }
    fun getProfileFontId(): String = encryptedPrefs.getString(KEY_PROFILE_FONT, "roboto_flex") ?: "roboto_flex"
    fun setProfileFontId(value: String) { encryptedPrefs.edit().putString(KEY_PROFILE_FONT, value).apply() }
    fun getProfileFontWidth(): Float = encryptedPrefs.getFloat(KEY_PROFILE_WIDTH, 100f)
    fun setProfileFontWidth(value: Float) { encryptedPrefs.edit().putFloat(KEY_PROFILE_WIDTH, value).apply() }
    fun getProfileFontGrade(): Int = encryptedPrefs.getInt(KEY_PROFILE_GRADE, 0)
    fun setProfileFontGrade(value: Int) { encryptedPrefs.edit().putInt(KEY_PROFILE_GRADE, value).apply() }
    fun getProfileFontWeight(): Int = encryptedPrefs.getInt(KEY_PROFILE_WEIGHT, 650)
    fun setProfileFontWeight(value: Int) { encryptedPrefs.edit().putInt(KEY_PROFILE_WEIGHT, value).apply() }
    fun getProfileOpticalSize(): Float = encryptedPrefs.getFloat(KEY_PROFILE_OPSZ, 14f)
    fun setProfileOpticalSize(value: Float) { encryptedPrefs.edit().putFloat(KEY_PROFILE_OPSZ, value).apply() }
    fun getProfileBio(): String = encryptedPrefs.getString(KEY_PROFILE_BIO, "") ?: ""
    fun setProfileBio(value: String) { encryptedPrefs.edit().putString(KEY_PROFILE_BIO, value.take(300)).apply() }

    fun setApplicationForeground(isForeground: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_APP_FOREGROUND, isForeground).apply()
    }

    fun isApplicationForeground(): Boolean = encryptedPrefs.getBoolean(KEY_APP_FOREGROUND, false)

    fun getFavoriteFriendIds(): Set<String> =
        encryptedPrefs.getStringSet(KEY_FAVORITE_FRIEND_IDS, emptySet())?.toSet() ?: emptySet()

    fun saveFavoriteFriendIds(ids: Set<String>) {
        encryptedPrefs.edit().putStringSet(KEY_FAVORITE_FRIEND_IDS, ids.toSet()).apply()
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        encryptedPrefs.edit()
            .putString(KEY_SUPABASE_URL, url)
            .putString(KEY_SUPABASE_ANON_KEY, anonKey)
            .apply()
    }

    fun getSupabaseUrl(): String? = encryptedPrefs.getString(KEY_SUPABASE_URL, null)
    fun getSupabaseAnonKey(): String? = encryptedPrefs.getString(KEY_SUPABASE_ANON_KEY, null)

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE = "shieldchat_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_USER_ID = "pref_user_id"
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DISPLAY_NAME = "pref_display_name"
        private const val KEY_TAG = "pref_tag"
        private const val KEY_AVATAR_PATH = "pref_avatar_path"
        private const val KEY_FAVORITE_FRIEND_IDS = "pref_favorite_friend_ids"
        private const val KEY_REGISTERED = "pref_is_registered"
        private const val KEY_SUPABASE_AUTH_USER_ID = "pref_supabase_auth_user_id"
        private const val KEY_APP_FOREGROUND = "pref_app_foreground"
        private const val KEY_VOICE_OF_USER = "pref_voice_of_user_enabled"
        private const val KEY_EDGE_DOCK_ENABLED = "pref_edge_dock_enabled"
        private const val KEY_PROFILE_BACKGROUND = "pref_profile_background"
        private const val KEY_PROFILE_FONT = "pref_profile_font"
        private const val KEY_PROFILE_WIDTH = "pref_profile_font_width"
        private const val KEY_PROFILE_GRADE = "pref_profile_font_grade"
        private const val KEY_PROFILE_WEIGHT = "pref_profile_font_weight"
        private const val KEY_PROFILE_OPSZ = "pref_profile_font_opsz"
        private const val KEY_PROFILE_BIO = "pref_profile_bio"
        private const val KEY_SUPABASE_URL = "pref_supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "pref_supabase_anon_key"
    }
}
