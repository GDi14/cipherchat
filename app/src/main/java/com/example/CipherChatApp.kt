package com.example

import android.app.Application
import android.util.Log

class CipherChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CipherChatApp", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            prevHandler?.uncaughtException(thread, throwable)
        }
    }
}
