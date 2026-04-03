package com.bitchat.android.util

/**
 * Normalizes user-entered peer identifiers for starting a private (Noise) chat.
 * Mesh peer IDs are the first 16 hex characters of the Noise identity fingerprint.
 */
object PeerIdInputNormalizer {

    private val hexLine = Regex("^[0-9a-f]+$")

    enum class ParseError {
        EMPTY,
        NOT_HEX,
        TOO_SHORT
    }

    sealed class ParseResult {
        data class Ok(val peerId: String) : ParseResult()
        data class Err(val error: ParseError) : ParseResult()
    }

    fun parse(raw: String): ParseResult {
        val compact = raw.trim().replace(Regex("[\\s-]"), "").lowercase()
        if (compact.isEmpty()) {
            return ParseResult.Err(ParseError.EMPTY)
        }

        if (compact.startsWith("nostr_") || compact.startsWith("nostr:")) {
            return ParseResult.Ok(compact)
        }

        if (!hexLine.matches(compact)) {
            return ParseResult.Err(ParseError.NOT_HEX)
        }
        if (compact.length < 16) {
            return ParseResult.Err(ParseError.TOO_SHORT)
        }

        return ParseResult.Ok(compact.take(16))
    }
}
