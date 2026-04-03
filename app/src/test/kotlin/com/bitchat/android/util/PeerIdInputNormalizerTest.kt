package com.bitchat.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerIdInputNormalizerTest {

    @Test
    fun parse_16_hex() {
        val r = PeerIdInputNormalizer.parse("  A1B2c3d4e5f67890 ")
        assertTrue(r is PeerIdInputNormalizer.ParseResult.Ok)
        assertEquals("a1b2c3d4e5f67890", (r as PeerIdInputNormalizer.ParseResult.Ok).peerId)
    }

    @Test
    fun parse_long_hex_takes_prefix() {
        val r = PeerIdInputNormalizer.parse("a1b2c3d4e5f67890abcdef00")
        assertEquals("a1b2c3d4e5f67890", (r as PeerIdInputNormalizer.ParseResult.Ok).peerId)
    }

    @Test
    fun parse_nostr_alias() {
        val r = PeerIdInputNormalizer.parse("nostr_abc123")
        assertEquals("nostr_abc123", (r as PeerIdInputNormalizer.ParseResult.Ok).peerId)
    }

    @Test
    fun parse_too_short() {
        val r = PeerIdInputNormalizer.parse("abc")
        assertTrue(r is PeerIdInputNormalizer.ParseResult.Err)
    }
}
