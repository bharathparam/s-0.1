package com.bitchat.android.mapfeature

data class TrackPoint(
    val id: Long = 0,
    val trackId: Long, // Group points by a track session ID
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
