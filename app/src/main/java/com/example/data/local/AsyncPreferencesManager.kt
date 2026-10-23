package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cipherchat_async_prefs")

/**
 * Non-blocking, Jetpack DataStore-backed preference manager running on Dispatchers.IO.
 * Avoids main-thread KeyStore IPC stalling on startup.
 */
class AsyncPreferencesManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    val isRegisteredFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REGISTERED] ?: false
    }

    val isRegisteredState = isRegisteredFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val userIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    suspend fun setRegistered(registered: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_REGISTERED] = registered
        }
    }

    suspend fun setUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
        }
    }

    companion object {
        private val KEY_REGISTERED = booleanPreferencesKey("async_is_registered")
        private val KEY_USER_ID = stringPreferencesKey("async_user_id")

        @Volatile
        private var INSTANCE: AsyncPreferencesManager? = null

        fun getInstance(context: Context): AsyncPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AsyncPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
