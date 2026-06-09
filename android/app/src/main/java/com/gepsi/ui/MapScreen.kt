package com.gepsi.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gepsi.location.TrackingService
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onOpenRoutes: () -> Unit) {
    val ctx = LocalContext.current
    val vm: MapViewModel = viewModel()
    val activeId by vm.activeRouteId.collectAsStateWithLifecycle()
    val points by vm.livePoints.collectAsStateWithLifecycle()
    val notes by vm.liveNotes.collectAsStateWithLifecycle()

    val requester = rememberPermissionRequester()
    ensureFirstRunPermissions(requester)

    var noteSheetOpen by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(60.1699, 24.9384))
        }
    }
    val polyline = remember { Polyline().apply { outlinePaint.color = Color.argb(220, 33, 150, 243); outlinePaint.strokeWidth = 10f } }

    LaunchedEffect(mapView) {
        mapView.overlays.add(polyline)
    }

    LaunchedEffect(points) {
        polyline.setPoints(points.map { GeoPoint(it.lat, it.lon) })
        if (points.isNotEmpty()) {
            val last = points.last()
            mapView.controller.animateTo(GeoPoint(last.lat, last.lon))
        }
        mapView.invalidate()
    }

    LaunchedEffect(notes) {
        mapView.overlays.removeAll { it is Marker && it.id?.startsWith("note-") == true }
        for (n in notes) {
            val m = Marker(mapView).apply {
                id = "note-${n.id}"
                position = GeoPoint(n.lat, n.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = n.text ?: "Voice note"
            }
            mapView.overlays.add(m)
        }
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeId != null) "Recording…" else "Gepsi") },
                actions = {
                    IconButton(onClick = onOpenRoutes) {
                        Icon(Icons.Default.List, contentDescription = "Routes")
                    }
                }
            )
        },
        floatingActionButton = {
            val recording = activeId != null
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (recording) {
                    FloatingActionButton(onClick = { noteSheetOpen = true }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "Add note")
                    }
                }
                ExtendedFloatingActionButton(
                    text = { Text(if (recording) "Stop" else "Start") },
                    icon = {
                        Icon(
                            if (recording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        if (recording) {
                            ctx.startService(TrackingService.stopIntent(ctx))
                        } else {
                            if (!requester.isGranted()) {
                                requester.request()
                                return@ExtendedFloatingActionButton
                            }
                            val intent = TrackingService.startIntent(ctx, defaultRouteName())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(ctx, intent)
                            } else {
                                ctx.startService(intent)
                            }
                        }
                    }
                )
            }
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (noteSheetOpen) {
        val last = points.lastOrNull()
        NoteSheet(
            onDismiss = { noteSheetOpen = false },
            onSave = { text, voicePath ->
                if (last != null) vm.addNote(text, voicePath, last.lat, last.lon)
                noteSheetOpen = false
            }
        )
    }
}

private fun defaultRouteName(): String {
    val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return "Walk ${now.format(java.util.Date())}"
}
