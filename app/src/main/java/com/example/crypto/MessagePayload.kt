package com.example.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MessagePayload(
    val text: String = "",
    val replyToId: String? = null,
    val replySnippet: String? = null,
    val media: MediaPayload? = null,
    val textEmphasisRanges: List<TextEmphasisRange> = emptyList()
)

@Serializable
data class TextEmphasisRange(val start: Int, val end: Int)

@Serializable
data class MediaPayload(
    val path: String,
    val mimeType: String,
    val key: String,
    val iv: String,
    val durationSeconds: Int = 0,
    val fileName: String? = null
)

object MessagePayloadCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(payload: MessagePayload): ByteArray {
        return json.encodeToString(payload).toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): MessagePayload {
        val str = String(bytes, Charsets.UTF_8)
        return try {
            json.decodeFromString<MessagePayload>(str)
        } catch (e: Exception) {
            // Fallback for simple legacy text
            MessagePayload(text = str)
        }
    }

    fun encodeEmphasisRanges(ranges: List<TextEmphasisRange>): String = json.encodeToString(ranges)

    fun decodeEmphasisRanges(encoded: String): List<TextEmphasisRange> = try {
        json.decodeFromString<List<TextEmphasisRange>>(encoded)
    } catch (_: Exception) {
        emptyList()
    }
}
