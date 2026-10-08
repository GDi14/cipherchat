package com.example.data.remote

import android.content.Context
import com.example.data.local.SecurePreferencesManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.MoshiSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Provides access to the official Supabase Kotlin client with Postgrest, Realtime, and Storage.
 * Supabase URL and Anon Key can be dynamically configured via Settings or SecurePreferences.
 * Service role keys are NEVER allowed or placed in the client application.
 */
class SupabaseClientProvider(
    private val context: Context,
    private val securePrefs: SecurePreferencesManager
) {
    init {
        val savedUrl = securePrefs.getSupabaseUrl()
        if (savedUrl.isNullOrBlank() || savedUrl == "https://example.supabase.co") {
            securePrefs.saveSupabaseConfig(DEFAULT_SUPABASE_URL, DEFAULT_SUPABASE_ANON_KEY)
        }
    }

    // Default fallback credentials - can be configured in app Settings
    var currentUrl: String = securePrefs.getSupabaseUrl() ?: DEFAULT_SUPABASE_URL
        private set
    var currentAnonKey: String = securePrefs.getSupabaseAnonKey() ?: DEFAULT_SUPABASE_ANON_KEY
        private set

    @Volatile
    private var clientInstance: SupabaseClient? = null

    fun getClient(): SupabaseClient {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: buildClient(currentUrl, currentAnonKey).also { clientInstance = it }
        }
    }

    fun updateCredentials(url: String, anonKey: String) {
        synchronized(this) {
            currentUrl = url.trim()
            currentAnonKey = anonKey.trim()
            securePrefs.saveSupabaseConfig(currentUrl, currentAnonKey)
            clientInstance = buildClient(currentUrl, currentAnonKey)
        }
    }

    private fun buildClient(url: String, key: String): SupabaseClient {
        val safeUrl = if (url.isBlank() || !url.startsWith("http")) "https://example.supabase.co" else url
        val safeKey = if (key.isBlank()) "placeholder-anon-key" else key

        return createSupabaseClient(
            supabaseUrl = safeUrl,
            supabaseKey = safeKey
        ) {
            httpEngine = OkHttp.create()
            defaultSerializer = MoshiSerializer()
            install(Auth) {
                // Keep refresh-token rotation and session persistence enabled for native sign-in.
                autoLoadFromStorage = true
                autoSaveToStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    val isConfigured: Boolean
        get() = currentUrl.isNotBlank() &&
                !currentUrl.contains("example.supabase.co") &&
                currentAnonKey.isNotBlank() &&
                currentAnonKey != "placeholder-anon-key"

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://tlmxxzrgmbeojejnwebz.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_CqGZ8X8wpvUGMI9OeFwSsA_Zw5O6pJS"
    }
}
