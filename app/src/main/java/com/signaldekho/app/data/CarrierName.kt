package com.signaldekho.app.data

/**
 * Carrier strings from the OS often repeat the network type or the brand
 * ("JIO 4G — Jio"). Keep the most human-readable part.
 */
object CarrierName {
    private val networkTypeSuffix = Regex("""\s*\b[2345]G\b\s*""", RegexOption.IGNORE_CASE)

    fun clean(raw: String): String {
        val parts = raw.split("—", "-", "|").map { it.trim() }.filter { it.isNotBlank() }
        val candidate = parts.lastOrNull() ?: raw
        val stripped = candidate.replace(networkTypeSuffix, " ").trim()
        val name = stripped.ifBlank { candidate.trim() }
        return if (name.isBlank()) "SIM" else name
    }
}
