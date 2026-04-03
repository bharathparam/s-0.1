package com.bitchat.android.ledger

import com.bitchat.android.crypto.EncryptionService
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Binary payload for [com.bitchat.android.protocol.MessageType.LEDGER_RECORD].
 * Inner Ed25519 signature covers content hash, timestamp, creator, title, mime, size.
 */
data class LedgerRecordPayload(
    val contentHashSha256: ByteArray,
    val timestampMs: Long,
    val creatorPeerId: String,
    val title: String,
    val mimeType: String,
    val contentSize: Long,
    val signingPublicKey: ByteArray,
    val signature: ByteArray
) {
    fun canonicalSigningBytes(): ByteArray {
        val creator = creatorPeerId.toByteArray(Charsets.UTF_8)
        val titleBytes = title.toByteArray(Charsets.UTF_8)
        val mimeBytes = mimeType.toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(
            32 + 8 + 2 + creator.size + 2 + titleBytes.size + 2 + mimeBytes.size + 8
        ).order(ByteOrder.BIG_ENDIAN)
        buf.put(contentHashSha256)
        buf.putLong(timestampMs)
        buf.putShort(creator.size.toShort())
        buf.put(creator)
        buf.putShort(titleBytes.size.toShort())
        buf.put(titleBytes)
        buf.putShort(mimeBytes.size.toShort())
        buf.put(mimeBytes)
        buf.putLong(contentSize)
        return buf.array()
    }

    fun encode(): ByteArray {
        val canon = canonicalSigningBytes()
        val pk = signingPublicKey
        val sig = signature
        val out = ByteBuffer.allocate(4 + 1 + canon.size + 2 + pk.size + 2 + sig.size)
            .order(ByteOrder.BIG_ENDIAN)
        out.put("BLED".toByteArray(Charsets.US_ASCII))
        out.put(1.toByte()) // version
        out.put(canon)
        out.putShort(pk.size.toShort())
        out.put(pk)
        out.putShort(sig.size.toShort())
        out.put(sig)
        return out.array()
    }

    companion object {
        fun decode(data: ByteArray): LedgerRecordPayload? {
            return try {
                if (data.size < 4 + 1 + 32 + 8) return null
                if (!data.copyOfRange(0, 4).contentEquals("BLED".toByteArray(Charsets.US_ASCII))) return null
                if (data[4].toInt() != 1) return null
                var off = 5
                val hash = data.copyOfRange(off, off + 32)
                off += 32
                val ts = ByteBuffer.wrap(data, off, 8).order(ByteOrder.BIG_ENDIAN).long
                off += 8
                if (off + 2 > data.size) return null
                val clen = ByteBuffer.wrap(data, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                off += 2
                if (off + clen > data.size) return null
                val creator = String(data, off, clen, Charsets.UTF_8)
                off += clen
                if (off + 2 > data.size) return null
                val tlen = ByteBuffer.wrap(data, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                off += 2
                if (off + tlen > data.size) return null
                val title = String(data, off, tlen, Charsets.UTF_8)
                off += tlen
                if (off + 2 > data.size) return null
                val mlen = ByteBuffer.wrap(data, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                off += 2
                if (off + mlen > data.size) return null
                val mime = String(data, off, mlen, Charsets.UTF_8)
                off += mlen
                if (off + 8 > data.size) return null
                val size = ByteBuffer.wrap(data, off, 8).order(ByteOrder.BIG_ENDIAN).long
                off += 8
                if (off + 2 > data.size) return null
                val pkLen = ByteBuffer.wrap(data, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                off += 2
                if (off + pkLen > data.size) return null
                val pk = data.copyOfRange(off, off + pkLen)
                off += pkLen
                if (off + 2 > data.size) return null
                val sigLen = ByteBuffer.wrap(data, off, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                off += 2
                if (off + sigLen > data.size) return null
                val sig = data.copyOfRange(off, off + sigLen)
                LedgerRecordPayload(hash, ts, creator, title, mime, size, pk, sig)
            } catch (_: Exception) {
                null
            }
        }

        fun buildSigned(
            contentHashSha256: ByteArray,
            timestampMs: Long,
            creatorPeerId: String,
            title: String,
            mimeType: String,
            contentSize: Long,
            encryptionService: EncryptionService
        ): LedgerRecordPayload? {
            val pk = encryptionService.getSigningPublicKey() ?: return null
            val unsigned = LedgerRecordPayload(
                contentHashSha256 = contentHashSha256,
                timestampMs = timestampMs,
                creatorPeerId = creatorPeerId,
                title = title,
                mimeType = mimeType,
                contentSize = contentSize,
                signingPublicKey = pk,
                signature = ByteArray(0)
            )
            val sig = encryptionService.signData(unsigned.canonicalSigningBytes()) ?: return null
            return unsigned.copy(signature = sig)
        }
    }
}
