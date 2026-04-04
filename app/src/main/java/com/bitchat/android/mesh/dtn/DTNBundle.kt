package com.bitchat.android.mesh.dtn

import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.util.toHexString
import java.security.MessageDigest

/**
 * A Delay-Tolerant Networking bundle.
 *
 * Wraps a raw [BitchatPacket] with epidemic-routing metadata so that
 * messages can survive network partitions and hop across devices over
 * time until they reach their destination.
 *
 * Design mirrors RFC 5050 Bundle Protocol concepts:
 * - unique bundle ID derived from content hash
 * - creation timestamp + absolute expiry (TTL)
 * - hop counter to prevent infinite circulation
 * - "seen-by" peer set for loop avoidance
 */
data class DTNBundle(
    /** Content-derived unique ID (SHA-256 of senderID + timestamp + first 64 bytes of payload). */
    val bundleId: String,

    /** The original wire packet to be delivered. */
    val packet: BitchatPacket,

    /** Epoch millis when this bundle was created. */
    val createdAtMs: Long = System.currentTimeMillis(),

    /** Epoch millis after which the bundle should be discarded. */
    val expiresAtMs: Long = createdAtMs + DEFAULT_TTL_MS,

    /** Number of hops this bundle has already traversed. */
    var hopCount: Int = 0,

    /** Peer IDs that have already received a copy of this bundle (loop avoidance). */
    val seenBy: MutableSet<String> = mutableSetOf(),

    /**
     * Hex string of the intended final recipient's peer ID.
     * `null` means broadcast / public message destined for all.
     */
    val destinationPeerId: String?,

    /** True once we receive confirmation that the final recipient got this bundle. */
    var delivered: Boolean = false,

    /** The original sender's peer ID (hex). */
    val sourcePeerId: String
) {
    companion object {
        /** Default bundle lifetime: 24 hours. */
        const val DEFAULT_TTL_MS: Long = 24 * 60 * 60 * 1000L

        /** Maximum number of hops before a bundle is dropped. */
        const val MAX_HOPS: Int = 20

        /** Maximum bundles any single device should store. */
        const val MAX_BUNDLES: Int = 500

        /**
         * Derive a deterministic bundle ID from a packet's identity fields
         * so that the same logical message always produces the same ID,
         * enabling deduplication across the mesh.
         */
        fun deriveBundleId(packet: BitchatPacket): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(packet.senderID)
            digest.update(packet.timestamp.toLong().toBigInteger().toByteArray())
            // Use up to 64 bytes of payload for uniqueness while keeping it fast
            val payloadSlice = packet.payload.take(64).toByteArray()
            digest.update(payloadSlice)
            packet.recipientID?.let { digest.update(it) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Create a [DTNBundle] from a [BitchatPacket], auto-deriving metadata.
         */
        fun fromPacket(
            packet: BitchatPacket,
            myPeerId: String,
            ttlMs: Long = DEFAULT_TTL_MS
        ): DTNBundle {
            val now = System.currentTimeMillis()
            val destHex = packet.recipientID?.toHexString()
            // Treat the special broadcast recipient as "no specific destination"
            val isBroadcast = destHex == "ffffffffffffffff"
            return DTNBundle(
                bundleId = deriveBundleId(packet),
                packet = packet,
                createdAtMs = now,
                expiresAtMs = now + ttlMs,
                hopCount = 0,
                seenBy = mutableSetOf(myPeerId),
                destinationPeerId = if (isBroadcast) null else destHex,
                sourcePeerId = packet.senderID.toHexString()
            )
        }
    }

    /** Whether this bundle has exceeded its time-to-live. */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAtMs

    /** Whether the bundle has reached its maximum hop count. */
    fun isExhausted(): Boolean = hopCount >= MAX_HOPS

    /** Whether this bundle is still valid for forwarding. */
    fun isForwardable(): Boolean = !isExpired() && !isExhausted() && !delivered

    /** Record that a peer has seen this bundle and increment hop count. */
    fun recordHop(peerId: String) {
        seenBy.add(peerId)
        hopCount++
    }

    /** Check whether a given peer has already received this bundle. */
    fun hasBeenSeenBy(peerId: String): Boolean = seenBy.contains(peerId)
}
