package com.example

import android.app.Application
import android.util.Log
import com.example.crypto.SignalProtocolManager
import com.example.data.local.AppDatabase
import com.example.data.local.AsyncPreferencesManager
import com.example.data.local.SecurePreferencesManager
import com.example.data.remote.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CipherChatApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CipherChatApp", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            prevHandler?.uncaughtException(thread, throwable)
        }

        // Asynchronously pre-warm core databases, crypto key managers, and Supabase client on Dispatchers.IO
        applicationScope.launch {
            try {
                val secPrefs = SecurePreferencesManager.getInstance(applicationContext)
                AsyncPreferencesManager.getInstance(applicationContext)
                val db = AppDatabase.getInstance(applicationContext)
                // Force open SQLite WAL connection in background thread so frame 1 query is 0ms
                db.openHelper.writableDatabase
                SignalProtocolManager.getInstance(applicationContext, secPrefs)
                SupabaseClientProvider(applicationContext, secPrefs).getClient()
                Log.d("CipherChatApp", "Async background pre-warming completed successfully")
            } catch (e: Exception) {
                Log.w("CipherChatApp", "Notice during async pre-warming: ${e.message}")
            }
        }
    }
}
