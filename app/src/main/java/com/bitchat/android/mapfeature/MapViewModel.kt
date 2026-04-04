package com.bitchat.android.mapfeature

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.android.geohash.Geohash
import com.bitchat.android.geohash.LocationProvider
import com.bitchat.android.geohash.SystemLocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = MapDatabaseHelper(application)
    private val locationProvider: LocationProvider = SystemLocationProvider(application)

    val bookmarks: StateFlow<List<Bookmark>> = dbHelper.bookmarksFlow

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()
    
    private var currentTrackId: Long = 0L

    init {
        // Request initial location
        locationProvider.getLastKnownLocation { loc ->
            loc?.let { _currentLocation.value = it }
        }
    }

    fun addBookmark(name: String, description: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val geohash = try {
                Geohash.encode(lat, lon, 9)
            } catch (e: Exception) {
                ""
            }
            val bookmark = Bookmark(
                name = name,
                description = description,
                latitude = lat,
                longitude = lon,
                geohash = geohash
            )
            dbHelper.insertBookmark(bookmark)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            dbHelper.deleteBookmark(bookmark)
        }
    }

    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        currentTrackId = System.currentTimeMillis()
        
        locationProvider.requestLocationUpdates(5000L, 5f) { location ->
            _currentLocation.value = location
            if (_isTracking.value) {
                viewModelScope.launch {
                    dbHelper.insertTrackPoint(TrackPoint(
                        trackId = currentTrackId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude
                    ))
                }
            }
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) return
        _isTracking.value = false
        locationProvider.removeLocationUpdates { }
        locationProvider.cancel()
    }

    fun requestFreshLocation() {
        locationProvider.requestFreshLocation { loc ->
            loc?.let { _currentLocation.value = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.cancel()
    }
}
