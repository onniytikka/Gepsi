package com.gepsi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gepsi.GepsiApp
import com.gepsi.data.Note
import com.gepsi.data.Route
import com.gepsi.data.TrackPoint
import com.gepsi.location.TrackingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GepsiApp.get().repository

    val activeRouteId: StateFlow<Long?> = TrackingState.activeRouteId

    val livePoints: StateFlow<List<TrackPoint>> = activeRouteId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observePoints(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liveNotes: StateFlow<List<Note>> = activeRouteId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeNotes(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addNote(text: String?, voicePath: String?, lat: Double, lon: Double) {
        val routeId = activeRouteId.value ?: return
        viewModelScope.launch {
            repo.addNote(
                Note(
                    routeId = routeId,
                    lat = lat,
                    lon = lon,
                    ts = System.currentTimeMillis(),
                    text = text?.takeIf { it.isNotBlank() },
                    voicePath = voicePath,
                )
            )
        }
    }
}
