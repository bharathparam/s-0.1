package com.bitchat.android.ledger

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class LedgerEntryRow(
    val contentHashHex: String,
    val timestampMs: Long,
    val creatorPeerId: String,
    val title: String,
    val mimeType: String,
    val contentSize: Long,
    val hasBlob: Boolean,
    val isLocal: Boolean
)

/**
 * Append-only logical ledger with content-addressed blob storage (hash -> file).
 */
class LedgerRepository private constructor(private val appContext: Context) : SQLiteOpenHelper(
    appContext,
    "swarm_net_ledger.db",
    null,
    1
) {
    private val _entries = MutableStateFlow<List<LedgerEntryRow>>(emptyList())
    val entries: StateFlow<List<LedgerEntryRow>> = _entries.asStateFlow()

    private val blobsDir: File = File(appContext.filesDir, "ledger_blobs").apply { mkdirs() }

    companion object {
        private const val TAG = "LedgerRepository"
        @Volatile private var inst: LedgerRepository? = null

        fun getInstance(context: Context): LedgerRepository {
            return inst ?: synchronized(this) {
                inst ?: LedgerRepository(context.applicationContext).also {
                    inst = it
                    it.refreshFlow()
                }
            }
        }

        fun sha256Hex(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(bytes).joinToString("") { b -> "%02x".format(b) }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ledger_entries (
                hash_hex TEXT PRIMARY KEY,
                ts INTEGER NOT NULL,
                creator_id TEXT NOT NULL,
                title TEXT NOT NULL,
                mime TEXT NOT NULL,
                content_size INTEGER NOT NULL,
                has_blob INTEGER NOT NULL,
                is_local INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun blobFile(hashHex: String): File = File(blobsDir, hashHex.lowercase())

    fun writeBlob(hashHex: String, bytes: ByteArray): Boolean {
        return try {
            val f = blobFile(hashHex)
            FileOutputStream(f).use { it.write(bytes) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeBlob failed: ${e.message}")
            false
        }
    }

    fun hasBlob(hashHex: String): Boolean = blobFile(hashHex).isFile

    fun exists(hashHex: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM ledger_entries WHERE hash_hex=? LIMIT 1",
            arrayOf(hashHex.lowercase())
        ).use { it.moveToFirst() }
    }

    /**
     * Insert or ignore duplicate hash.
     */
    fun mergeRecord(
        hashHex: String,
        timestampMs: Long,
        creatorPeerId: String,
        title: String,
        mimeType: String,
        contentSize: Long,
        hasBlob: Boolean,
        isLocal: Boolean
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("hash_hex", hashHex.lowercase())
            put("ts", timestampMs)
            put("creator_id", creatorPeerId)
            put("title", title.take(512))
            put("mime", mimeType.take(200))
            put("content_size", contentSize)
            put("has_blob", if (hasBlob) 1 else 0)
            put("is_local", if (isLocal) 1 else 0)
        }
        db.insertWithOnConflict(
            "ledger_entries",
            null,
            cv,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        val patch = ContentValues().apply {
            put("ts", timestampMs)
            put("creator_id", creatorPeerId)
            put("title", title.take(512))
            put("mime", mimeType.take(200))
            put("content_size", contentSize)
            if (hasBlob) put("has_blob", 1)
        }
        db.update("ledger_entries", patch, "hash_hex=?", arrayOf(hashHex.lowercase()))
        if (isLocal) {
            db.execSQL(
                "UPDATE ledger_entries SET is_local=1 WHERE hash_hex=?",
                arrayOf(hashHex.lowercase())
            )
        }
        Log.d(TAG, "merged ledger record $hashHex")
        refreshFlow()
    }

    fun markHasBlob(hashHex: String) {
        writableDatabase.execSQL(
            "UPDATE ledger_entries SET has_blob=1 WHERE hash_hex=?",
            arrayOf(hashHex.lowercase())
        )
        refreshFlow()
    }

    fun refreshFlow() {
        val list = mutableListOf<LedgerEntryRow>()
        readableDatabase.rawQuery(
            "SELECT hash_hex, ts, creator_id, title, mime, content_size, has_blob, is_local FROM ledger_entries ORDER BY ts DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LedgerEntryRow(
                        contentHashHex = c.getString(0),
                        timestampMs = c.getLong(1),
                        creatorPeerId = c.getString(2),
                        title = c.getString(3),
                        mimeType = c.getString(4),
                        contentSize = c.getLong(5),
                        hasBlob = c.getInt(6) == 1,
                        isLocal = c.getInt(7) == 1
                    )
                )
            }
        }
        _entries.value = list
    }
}
