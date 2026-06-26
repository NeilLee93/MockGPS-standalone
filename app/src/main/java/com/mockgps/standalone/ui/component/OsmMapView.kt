// app/src/main/java/com/mockgps/standalone/ui/component/OsmMapView.kt
package com.mockgps.standalone.ui.component

import android.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun OsmMapView(
    lat: Double,
    lon: Double,
    walkerRadius: Float?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = rememberMapView()
    var marker by remember { mutableStateOf<Marker?>(null) }
    var circle by remember { mutableStateOf<Polygon?>(null) }

    // Wire up tap-to-place: recreate MapEventsOverlay when onLocationSelected changes
    DisposableEffect(mapView, onLocationSelected) {
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onLocationSelected(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(tapOverlay)
        onDispose {
            mapView.overlays.remove(tapOverlay)
        }
    }

    // Update pin + circle whenever lat/lon/walkerRadius changes
    LaunchedEffect(lat, lon, walkerRadius) {
        val gp = GeoPoint(lat, lon)

        // Remove old overlays
        marker?.let { mapView.overlays.remove(it) }
        circle?.let { mapView.overlays.remove(it) }

        // Add draggable marker
        val m = Marker(mapView).apply {
            position = gp
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(m: Marker) {}
                override fun onMarkerDragStart(m: Marker) {}
                override fun onMarkerDragEnd(m: Marker) {
                    onLocationSelected(m.position.latitude, m.position.longitude)
                }
            })
        }
        marker = m
        mapView.overlays.add(m)

        // Walker radius circle
        if (walkerRadius != null && walkerRadius > 0f) {
            val c = Polygon().apply {
                points = Polygon.pointsAsCircle(gp, walkerRadius.toDouble())
                fillPaint.color = Color.argb(40, 0, 120, 255)
                outlinePaint.color = Color.argb(180, 0, 120, 255)
                outlinePaint.strokeWidth = 3f
                isVisible = true
            }
            circle = c
            mapView.overlays.add(0, c)
        } else {
            circle = null
        }

        mapView.controller.animateTo(gp)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    // Safe in Activity-hosted Compose; would crash in Dialog/Fragment without proper wiring.
    val lifecycleOwner = context as LifecycleOwner

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    return mapView
}
