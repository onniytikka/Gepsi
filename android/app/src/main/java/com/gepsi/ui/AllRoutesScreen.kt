package com.gepsi.ui

import android.app.Application
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

data class RouteBundle(
    val route: Route,
    val points: List<TrackPoint>,
    val notes: List<Note>,
)

class AllRoutesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GepsiApp.get().repository

    private val _bundles = MutableStateFlow<List<RouteBundle>>(emptyList())
    val bundles: StateFlow<List<RouteBundle>> = _bundles.asStateFlow()

    init {
        viewModelScope.launch {
            val routes = repo.observeRoutes()
            routes.collect { rs ->
                _bundles.value = rs.map { r ->
                    RouteBundle(
                        route = r,
                        points = repo.pointsForRoute(r.id),
                        notes = repo.notesForRoute(r.id),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRoutesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val vm: AllRoutesViewModel = viewModel()
    val bundles by vm.bundles.collectAsStateWithLifecycle()
    var selectedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    LaunchedEffect(bundles) {
        mapView.overlays.clear()

        val allLats = mutableListOf<Double>()
        val allLons = mutableListOf<Double>()

        for ((idx, bundle) in bundles.withIndex()) {
            if (bundle.points.isEmpty()) continue
            val color = colorForRoute(bundle.route.id, idx)
            val line = Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 9f
                title = bundle.route.name
                setPoints(bundle.points.map { GeoPoint(it.lat, it.lon) })
            }
            mapView.overlays.add(line)
            allLats += bundle.points.map { it.lat }
            allLons += bundle.points.map { it.lon }

            for (n in bundle.notes) {
                val m = Marker(mapView).apply {
                    id = "note-${n.id}"
                    position = GeoPoint(n.lat, n.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = n.text ?: "Voice note"
                    snippet = bundle.route.name
                    setOnMarkerClickListener { _, _ ->
                        selectedNoteId = n.id
                        true
                    }
                }
                mapView.overlays.add(m)
            }
        }

        if (allLats.isNotEmpty()) {
            mapView.zoomToBoundingBox(
                BoundingBox(allLats.max(), allLons.max(), allLats.min(), allLons.min()),
                true,
                96,
            )
        }
        mapView.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All routes (${bundles.count { it.points.isNotEmpty() }})") },
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

    bundles.flatMap { it.notes }.find { it.id == selectedNoteId }?.let { n ->
        NoteDialog(note = n, onDismiss = { selectedNoteId = null })
    }
}

private fun colorForRoute(id: Long, idx: Int): Int {
    val hue = ((id * 47L + idx * 137L) % 360L).toFloat()
    val hsv = floatArrayOf(hue, 0.75f, 0.85f)
    return Color.HSVToColor(220, hsv)
}
