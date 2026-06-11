package com.gepsi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gepsi.GepsiApp
import com.gepsi.data.Route
import com.gepsi.exchange.PackageExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RouteListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GepsiApp.get().repository

    val routes: StateFlow<List<Route>> = repo.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    fun exportAll(onReady: (File) -> Unit) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            runCatching { PackageExporter(getApplication(), repo).exportAll() }
                .onSuccess(onReady)
            _exporting.value = false
        }
    }
}
