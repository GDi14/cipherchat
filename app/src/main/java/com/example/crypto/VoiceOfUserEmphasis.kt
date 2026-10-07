package com.example.crypto

/** Lightweight, local-only phrase selection for the Voice of User styling effect. */
object VoiceOfUserEmphasis {
    private val uppercasePhrase = Regex("(?<![\\p{L}\\p{N}])[\\p{Lu}][\\p{Lu}\\p{N}'’\\-]*(?:\\s+[\\p{Lu}][\\p{Lu}\\p{N}'’\\-]*)*(?![\\p{L}\\p{N}])")
    private val expressivePhrase = Regex(
        "(?<![\\p{L}\\p{N}])(?:holy\\s+dayum|oh\\s+my\\s+god|no\\s+way|so\\s+good|absolutely|amazing|awesome|incredible|fantastic|wonderful|brilliant|perfect|excellent|beautiful|insane|dayum|damn|great|nice|wow|love)(?![\\p{L}\\p{N}])",
        RegexOption.IGNORE_CASE
    )

    fun detect(text: String): List<TextEmphasisRange> {
        uppercasePhrase.findAll(text).firstOrNull { match ->
            match.value.count(Char::isLetter) >= 3 && match.value.none(Char::isLowerCase)
        }?.let { return listOf(TextEmphasisRange(it.range.first, it.range.last + 1)) }
        return expressivePhrase.findAll(text).maxByOrNull { it.value.length }?.let {
            listOf(TextEmphasisRange(it.range.first, it.range.last + 1))
        }.orEmpty()
    }
}
