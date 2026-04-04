package com.bitchat.android.mapfeature

import android.content.Context
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitchat.android.ui.theme.BASE_FONT_SIZE
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToBookmarks: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val bookmarks by viewModel.bookmarks.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    // Init osmdroid config
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showAddBookmarkDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val center = mapView?.mapCenter
                    if (center != null && name.isNotBlank()) {
                        viewModel.addBookmark(name, desc, center.latitude, center.longitude)
                    }
                    showAddBookmarkDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    
                    val mapController = controller
                    mapController.setZoom(15.0)
                    
                    // Add My Location Overlay
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                    
                    // Initial center (rough approximation if no location yet)
                    mapController.setCenter(GeoPoint(0.0, 0.0))
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                mapView = view
                
                // Update bookmarks
                view.overlays.removeAll { it is Marker }
                bookmarks.forEach { bm ->
                    val marker = Marker(view)
                    marker.position = GeoPoint(bm.latitude, bm.longitude)
                    marker.title = bm.name
                    marker.snippet = bm.description
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    view.overlays.add(marker)
                }
                view.invalidate()
            }
        )

        // Floating controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    currentLocation?.let {
                        mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                    } ?: viewModel.requestFreshLocation()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.MyLocation, "Center on me")
            }
            
            FloatingActionButton(
                onClick = { showAddBookmarkDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.AddLocation, "Add bookmark at center")
            }

            FloatingActionButton(
                onClick = onNavigateToBookmarks,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.List, "Bookmarks list")
            }

            FloatingActionButton(
                onClick = { if (isTracking) viewModel.stopTracking() else viewModel.startTracking() },
                containerColor = if (isTracking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(if (isTracking) Icons.Default.StopCircle else Icons.Default.Terrain, "Toggle tracks")
            }
        }
    }
}
