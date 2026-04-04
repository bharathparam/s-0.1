package com.bitchat.android.mapfeature

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "swarmnet_map.db"
        const val DATABASE_VERSION = 1
        
        // Bookmarks
        const val TABLE_BOOKMARKS = "bookmarks"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_DESC = "description"
        const val COL_LAT = "latitude"
        const val COL_LON = "longitude"
        const val COL_GEOHASH = "geohash"
        const val COL_TIMESTAMP = "timestamp"

        // Tracks
        const val TABLE_TRACKS = "track_points"
        const val COL_TRACK_ID = "trackId"
        const val COL_ALT = "altitude"
    }

    private val _bookmarksFlow = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarksFlow = _bookmarksFlow.asStateFlow()

    init {
        refreshBookmarks()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_BOOKMARKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_DESC TEXT NOT NULL,
                $COL_LAT REAL NOT NULL,
                $COL_LON REAL NOT NULL,
                $COL_GEOHASH TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_TRACKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRACK_ID INTEGER NOT NULL,
                $COL_LAT REAL NOT NULL,
                $COL_LON REAL NOT NULL,
                $COL_ALT REAL NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRACKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        onCreate(db)
    }

    private fun refreshBookmarks() {
        val db = readableDatabase
        val list = mutableListOf<Bookmark>()
        val cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        with(cursor) {
            while (moveToNext()) {
                list.add(
                    Bookmark(
                        id = getLong(getColumnIndexOrThrow(COL_ID)),
                        name = getString(getColumnIndexOrThrow(COL_NAME)),
                        description = getString(getColumnIndexOrThrow(COL_DESC)),
                        latitude = getDouble(getColumnIndexOrThrow(COL_LAT)),
                        longitude = getDouble(getColumnIndexOrThrow(COL_LON)),
                        geohash = getString(getColumnIndexOrThrow(COL_GEOHASH)),
                        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            }
        }
        cursor.close()
        _bookmarksFlow.value = list
    }

    fun insertBookmark(bookmark: Bookmark): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, bookmark.name)
            put(COL_DESC, bookmark.description)
            put(COL_LAT, bookmark.latitude)
            put(COL_LON, bookmark.longitude)
            put(COL_GEOHASH, bookmark.geohash)
            put(COL_TIMESTAMP, bookmark.timestamp)
        }
        val id = db.insert(TABLE_BOOKMARKS, null, values)
        refreshBookmarks()
        return id
    }

    fun deleteBookmark(bookmark: Bookmark) {
        val db = writableDatabase
        db.delete(TABLE_BOOKMARKS, "$COL_ID = ?", arrayOf(bookmark.id.toString()))
        refreshBookmarks()
    }

    fun insertTrackPoint(point: TrackPoint) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TRACK_ID, point.trackId)
            put(COL_LAT, point.latitude)
            put(COL_LON, point.longitude)
            put(COL_ALT, point.altitude)
            put(COL_TIMESTAMP, point.timestamp)
        }
        db.insert(TABLE_TRACKS, null, values)
    }
}
