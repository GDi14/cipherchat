package com.example.crypto

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Striped Mutex manager for cryptographic session concurrency.
 * Ensures Double Ratchet state transitions, duplicate message checks, and database inserts
 * occur atomically per recipient address / conversation ID across FCM pushes, WebSockets, and workers.
 */
object CryptoConcurrencyManager {
    @PublishedApi
    internal val lockRegistry = ConcurrentHashMap<String, Mutex>()

    /**
     * Executes the [block] under a lock specific to [key] (e.g., recipientUserId or conversationId).
     */
    suspend inline fun <T> withLock(key: String, crossinline block: suspend () -> T): T {
        val lock = lockRegistry.computeIfAbsent(key) { Mutex() }
        return lock.withLock {
            block()
        }
    }

    /**
     * Clears unused mutex entries to prevent long-term memory growth.
     */
    fun pruneUnusedLocks() {
        val iterator = lockRegistry.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isLocked) {
                iterator.remove()
            }
        }
    }
}
