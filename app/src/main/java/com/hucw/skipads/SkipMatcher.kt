package com.hucw.skipads

import java.util.Locale

object SkipMatcher {

    private const val MAX_LENGTH = 12
    private val WHITESPACE = Regex("\\s+")

    fun normalize(raw: String): String =
        raw.replace(WHITESPACE, "").lowercase(Locale.ROOT)

    fun isSkipText(raw: String?, keywords: List<String>): Boolean {
        if (raw.isNullOrBlank()) return false
        val text = normalize(raw)
        if (text.length > MAX_LENGTH) return false
        if (keywords.isEmpty()) return false
        return keywords.any { keyword ->
            val normalized = normalize(keyword)
            normalized.isNotEmpty() && text.contains(normalized)
        }
    }
}
