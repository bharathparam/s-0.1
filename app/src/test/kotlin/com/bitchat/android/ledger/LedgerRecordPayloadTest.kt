package com.bitchat.android.ledger

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class LedgerRecordPayloadTest {

    @Test
    fun encode_decode_roundtrip() {
        val hash = MessageDigest.getInstance("SHA-256").digest("hello".toByteArray())
        val sig = ByteArray(64) { (it % 256).toByte() }
        val pk = ByteArray(32) { 3 }
        val payload = LedgerRecordPayload(
            contentHashSha256 = hash,
            timestampMs = 1_700_000_000L,
            creatorPeerId = "a1b2c3d4e5f67890",
            title = "Title",
            mimeType = "application/pdf",
            contentSize = 99L,
            signingPublicKey = pk,
            signature = sig
        )
        val bytes = payload.encode()
        val decoded = LedgerRecordPayload.decode(bytes)!!
        assertArrayEquals(hash, decoded.contentHashSha256)
        assertEquals(1_700_000_000L, decoded.timestampMs)
        assertEquals("a1b2c3d4e5f67890", decoded.creatorPeerId)
        assertEquals("Title", decoded.title)
        assertEquals("application/pdf", decoded.mimeType)
        assertEquals(99L, decoded.contentSize)
        assertArrayEquals(pk, decoded.signingPublicKey)
        assertArrayEquals(sig, decoded.signature)
    }
}
