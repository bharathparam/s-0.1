package com.bitchat.android.mapfeature

data class Bookmark(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val geohash: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
