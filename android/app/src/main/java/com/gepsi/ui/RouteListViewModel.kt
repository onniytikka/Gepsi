package com.gepsi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gepsi.GepsiApp
import com.gepsi.data.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RouteListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GepsiApp.get().repository

    val routes: StateFlow<List<Route>> = repo.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
