package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Manages device identity and authentication state securely using Android KeyStore-backed
 * EncryptedSharedPreferences.
 */
class SecurePreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "shieldchat_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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

    fun saveProfile(displayName: String, tag: String, avatarPath: String?) {
        encryptedPrefs.edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_TAG, tag)
            .putString(KEY_AVATAR_PATH, avatarPath)
            .putBoolean(KEY_REGISTERED, true)
            .apply()
    }

    fun getDisplayName(): String? = encryptedPrefs.getString(KEY_DISPLAY_NAME, null)
    fun getTag(): String? = encryptedPrefs.getString(KEY_TAG, null)
    fun getAvatarPath(): String? = encryptedPrefs.getString(KEY_AVATAR_PATH, null)
    fun isRegistered(): Boolean = encryptedPrefs.getBoolean(KEY_REGISTERED, false)
    fun isVoiceOfUserEnabled(): Boolean = encryptedPrefs.getBoolean(KEY_VOICE_OF_USER, true)
    fun setVoiceOfUserEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_VOICE_OF_USER, enabled).apply()
    }

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
        private const val KEY_USER_ID = "pref_user_id"
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DISPLAY_NAME = "pref_display_name"
        private const val KEY_TAG = "pref_tag"
        private const val KEY_AVATAR_PATH = "pref_avatar_path"
        private const val KEY_FAVORITE_FRIEND_IDS = "pref_favorite_friend_ids"
        private const val KEY_REGISTERED = "pref_is_registered"
        private const val KEY_VOICE_OF_USER = "pref_voice_of_user_enabled"
        private const val KEY_SUPABASE_URL = "pref_supabase_url"
        private const val KEY_SUPABASE_ANON_KEY = "pref_supabase_anon_key"
    }
}
