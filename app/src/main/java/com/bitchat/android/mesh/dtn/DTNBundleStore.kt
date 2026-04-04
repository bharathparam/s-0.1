package com.bitchat.android.mesh.dtn

import android.content.Context
import android.util.Log
import com.bitchat.android.identity.SecureIdentityStateManager
import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.protocol.BinaryProtocol
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections

/**
 * Persistent store for DTN bundles.
 *
 * Bundles are kept in memory for fast access and periodically persisted to
 * encrypted local storage via [SecureIdentityStateManager] so they survive
 * app restarts and device reboots.
 *
 * The store enforces capacity limits ([DTNBundle.MAX_BUNDLES]) and automatic
 * TTL-based expiry to prevent memory exhaustion.
 */
class DTNBundleStore private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DTNBundleStore"
        private const val STORAGE_KEY = "dtn_bundle_store_v1"

        @Volatile
        private var INSTANCE: DTNBundleStore? = null

        fun getInstance(context: Context): DTNBundleStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DTNBundleStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = Gson()
    private val secure = SecureIdentityStateManager(context)

    /** In-memory bundle map: bundleId → DTNBundle */
    private val bundles: MutableMap<String, DTNBundle> =
        Collections.synchronizedMap(LinkedHashMap())

    /** IDs of bundles that were delivered (kept for dedup even after bundle removal). */
    private val deliveredIds: MutableSet<String> =
        Collections.synchronizedSet(LinkedHashSet())

    init {
        load()
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Store a new bundle. Returns `true` if it was actually stored (not a duplicate).
     */
    @Synchronized
    fun store(bundle: DTNBundle): Boolean {
        if (bundles.containsKey(bundle.bundleId)) {
            // Merge seen-by sets for existing bundle
            bundles[bundle.bundleId]?.seenBy?.addAll(bundle.seenBy)
            return false
        }
        if (deliveredIds.contains(bundle.bundleId)) {
            Log.d(TAG, "Bundle ${bundle.bundleId.take(12)} already delivered, skipping")
            return false
        }
        if (!bundle.isForwardable()) {
            Log.d(TAG, "Bundle ${bundle.bundleId.take(12)} not forwardable, skipping")
            return false
        }

        bundles[bundle.bundleId] = bundle

        // Enforce capacity limit – evict oldest bundles first
        while (bundles.size > DTNBundle.MAX_BUNDLES) {
            val oldestKey = bundles.keys.firstOrNull() ?: break
            bundles.remove(oldestKey)
            Log.d(TAG, "Evicted oldest bundle $oldestKey to stay under capacity")
        }

        persist()
        Log.d(TAG, "Stored DTN bundle ${bundle.bundleId.take(12)} " +
                "(dest=${bundle.destinationPeerId?.take(8) ?: "broadcast"}, " +
                "hops=${bundle.hopCount}, bundles=${bundles.size})")
        return true
    }

    /**
     * Return all forwardable bundles that the given peer has NOT yet seen.
     */
    @Synchronized
    fun getBundlesForPeer(peerId: String): List<DTNBundle> {
        purgeExpired()
        return bundles.values.filter { b ->
            b.isForwardable() && !b.hasBeenSeenBy(peerId)
        }
    }

    /**
     * Return bundles specifically destined for a given peer ID.
     */
    @Synchronized
    fun getBundlesDestinedFor(peerId: String): List<DTNBundle> {
        purgeExpired()
        return bundles.values.filter { b ->
            b.isForwardable() && b.destinationPeerId == peerId
        }
    }

    /**
     * Mark a bundle as delivered and remove it from active storage.
     */
    @Synchronized
    fun markDelivered(bundleId: String) {
        bundles[bundleId]?.delivered = true
        bundles.remove(bundleId)
        deliveredIds.add(bundleId)
        trimDeliveredIds()
        persist()
        Log.d(TAG, "Bundle $bundleId marked delivered")
    }

    /**
     * Record that a peer has seen a specific bundle (to avoid re-sending).
     */
    @Synchronized
    fun recordPeerSeen(bundleId: String, peerId: String) {
        bundles[bundleId]?.seenBy?.add(peerId)
    }

    /** Check if we already know about a bundle ID. */
    @Synchronized
    fun contains(bundleId: String): Boolean =
        bundles.containsKey(bundleId) || deliveredIds.contains(bundleId)

    /** Total number of active (non-expired, non-delivered) bundles. */
    @Synchronized
    fun activeBundleCount(): Int {
        purgeExpired()
        return bundles.size
    }

    /** Get a snapshot of all bundle IDs currently held. */
    @Synchronized
    fun getAllBundleIds(): Set<String> = bundles.keys.toSet()

    /** Purge expired bundles from memory and disk. */
    @Synchronized
    fun purgeExpired() {
        val before = bundles.size
        bundles.entries.removeAll { (_, b) -> b.isExpired() || b.isExhausted() }
        val removed = before - bundles.size
        if (removed > 0) {
            Log.d(TAG, "Purged $removed expired/exhausted bundles")
            persist()
        }
    }

    /** Clear everything (used during debug/reset). */
    @Synchronized
    fun clear() {
        bundles.clear()
        deliveredIds.clear()
        persist()
        Log.d(TAG, "DTN store cleared")
    }

    /** Debug summary. */
    fun getDebugInfo(): String = buildString {
        appendLine("=== DTN Bundle Store ===")
        appendLine("Active bundles: ${bundles.size}")
        appendLine("Delivered IDs tracked: ${deliveredIds.size}")
        val now = System.currentTimeMillis()
        bundles.values.take(10).forEachIndexed { i, b ->
            val ageMin = (now - b.createdAtMs) / 60_000
            val ttlMin = (b.expiresAtMs - now) / 60_000
            appendLine("  [$i] id=${b.bundleId.take(12)} dest=${b.destinationPeerId?.take(8) ?: "ALL"} " +
                    "hops=${b.hopCount} seen=${b.seenBy.size} age=${ageMin}m ttl=${ttlMin}m")
        }
        if (bundles.size > 10) appendLine("  ... and ${bundles.size - 10} more")
    }

    // ── Persistence ──────────────────────────────────────────────────────

    /**
     * Serializable envelope for JSON persistence.
     * Packets are stored as Base64-encoded binary wire format.
     */
    private data class BundleEnvelope(
        val bundleId: String,
        val packetBase64: String,
        val createdAtMs: Long,
        val expiresAtMs: Long,
        val hopCount: Int,
        val seenBy: List<String>,
        val destinationPeerId: String?,
        val sourcePeerId: String,
        val delivered: Boolean
    )

    private data class StorePayload(
        val bundles: List<BundleEnvelope>,
        val deliveredIds: List<String>
    )

    @Synchronized
    private fun persist() {
        try {
            val envelopes = bundles.values.mapNotNull { b ->
                val wire = b.packet.toBinaryData() ?: return@mapNotNull null
                val b64 = android.util.Base64.encodeToString(wire, android.util.Base64.NO_WRAP)
                BundleEnvelope(
                    bundleId = b.bundleId,
                    packetBase64 = b64,
                    createdAtMs = b.createdAtMs,
                    expiresAtMs = b.expiresAtMs,
                    hopCount = b.hopCount,
                    seenBy = b.seenBy.toList(),
                    destinationPeerId = b.destinationPeerId,
                    sourcePeerId = b.sourcePeerId,
                    delivered = b.delivered
                )
            }
            val payload = StorePayload(envelopes, deliveredIds.toList())
            secure.storeSecureValue(STORAGE_KEY, gson.toJson(payload))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist DTN store: ${e.message}")
        }
    }

    @Synchronized
    private fun load() {
        try {
            val json = secure.getSecureValue(STORAGE_KEY) ?: return
            val payload = gson.fromJson(json, StorePayload::class.java) ?: return

            bundles.clear()
            payload.bundles.forEach { env ->
                try {
                    val wire = android.util.Base64.decode(env.packetBase64, android.util.Base64.NO_WRAP)
                    val packet = BitchatPacket.fromBinaryData(wire) ?: return@forEach
                    val bundle = DTNBundle(
                        bundleId = env.bundleId,
                        packet = packet,
                        createdAtMs = env.createdAtMs,
                        expiresAtMs = env.expiresAtMs,
                        hopCount = env.hopCount,
                        seenBy = env.seenBy.toMutableSet(),
                        destinationPeerId = env.destinationPeerId,
                        sourcePeerId = env.sourcePeerId,
                        delivered = env.delivered
                    )
                    if (bundle.isForwardable()) {
                        bundles[bundle.bundleId] = bundle
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipped corrupt bundle envelope: ${e.message}")
                }
            }

            deliveredIds.clear()
            payload.deliveredIds.takeLast(5000).forEach { deliveredIds.add(it) }

            Log.d(TAG, "Loaded ${bundles.size} DTN bundles, ${deliveredIds.size} delivered IDs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load DTN store: ${e.message}")
        }
    }

    private fun trimDeliveredIds() {
        if (deliveredIds.size > 5000) {
            val iter = deliveredIds.iterator()
            var excess = deliveredIds.size - 5000
            while (excess > 0 && iter.hasNext()) {
                iter.next()
                iter.remove()
                excess--
            }
        }
    }
}
