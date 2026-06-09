package com.gepsi.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TrackingState {
    private val _activeRouteId = MutableStateFlow<Long?>(null)
    val activeRouteId: StateFlow<Long?> = _activeRouteId

    fun setActive(id: Long?) { _activeRouteId.value = id }
}
