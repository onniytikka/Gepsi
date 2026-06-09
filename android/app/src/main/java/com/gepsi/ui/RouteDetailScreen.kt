package com.gepsi.ui

import android.app.Application
import android.graphics.Color
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.gepsi.GepsiApp
import com.gepsi.data.Note
import com.gepsi.data.Route
import com.gepsi.data.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class RouteDetailViewModel(app: Application, private val routeId: Long) : AndroidViewModel(app) {
    private val repo = GepsiApp.get().repository

    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    private val _points = MutableStateFlow<List<TrackPoint>>(emptyList())
    val points: StateFlow<List<TrackPoint>> = _points.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        viewModelScope.launch {
            _route.value = repo.getRoute(routeId)
            _points.value = repo.pointsForRoute(routeId)
            _notes.value = repo.notesForRoute(routeId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(routeId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as Application
    val vm: RouteDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RouteDetailViewModel(app, routeId) }
        }
    )

    val route by vm.route.collectAsStateWithLifecycle()
    val points by vm.points.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }

    val polyline = remember {
        Polyline().apply {
            outlinePaint.color = Color.argb(220, 33, 150, 243)
            outlinePaint.strokeWidth = 10f
        }
    }

    LaunchedEffect(mapView) { mapView.overlays.add(polyline) }

    LaunchedEffect(points) {
        val geo = points.map { GeoPoint(it.lat, it.lon) }
        polyline.setPoints(geo)
        if (geo.isNotEmpty()) {
            val lats = geo.map { it.latitude }
            val lons = geo.map { it.longitude }
            mapView.zoomToBoundingBox(
                BoundingBox(lats.max(), lons.max(), lats.min(), lons.min()),
                true,
                64,
            )
        }
        mapView.invalidate()
    }

    LaunchedEffect(notes) {
        mapView.overlays.removeAll { it is Marker }
        for (n in notes) {
            val m = Marker(mapView).apply {
                id = "note-${n.id}"
                position = GeoPoint(n.lat, n.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = n.text ?: "Voice note"
                setOnMarkerClickListener { _, _ ->
                    selectedNote = n
                    true
                }
            }
            mapView.overlays.add(m)
        }
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }
    }

    selectedNote?.let { n ->
        NoteDialog(note = n, onDismiss = { selectedNote = null })
    }
}

@Composable
private fun NoteDialog(note: Note, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember { MediaPlayer() }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(note.id) {
        onDispose {
            runCatching { player.stop() }
            player.release()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            Text(note.text ?: if (note.voicePath != null) "Voice memo" else "(empty)")
        },
        confirmButton = {
            if (note.voicePath != null) {
                Button(onClick = {
                    if (playing) {
                        runCatching { player.stop() }
                        player.reset()
                        playing = false
                    } else {
                        runCatching {
                            player.reset()
                            player.setDataSource(note.voicePath)
                            player.prepare()
                            player.setOnCompletionListener { playing = false }
                            player.start()
                            playing = true
                        }
                    }
                }) {
                    Text(if (playing) "Stop" else "Play voice")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = if (note.voicePath != null) {
            { TextButton(onClick = onDismiss) { Text("Close") } }
        } else null,
    )
}
