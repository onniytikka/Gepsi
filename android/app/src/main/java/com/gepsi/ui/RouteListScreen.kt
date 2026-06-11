package com.gepsi.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gepsi.data.Route
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    onOpenRoute: (Long) -> Unit,
    onOpenAll: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: RouteListViewModel = viewModel()
    val routes by vm.routes.collectAsStateWithLifecycle()
    val exporting by vm.exporting.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (exporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = { vm.exportAll { file -> sharePackage(ctx, file) } },
                            enabled = routes.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export all data")
                        }
                    }
                    IconButton(onClick = onOpenAll, enabled = routes.isNotEmpty()) {
                        Icon(Icons.Default.Layers, contentDescription = "Show all on map")
                    }
                }
            )
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No routes yet. Tap Start on the map.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteRow(route = route, onClick = { onOpenRoute(route.id) })
                }
            }
        }
    }
}

private fun sharePackage(ctx: Context, file: File) {
    val uri = FileProvider.getUriForFile(ctx, "com.gepsi.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Share Gepsi data"))
}

@Composable
private fun RouteRow(route: Route, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            Text(fmt.format(Date(route.startTs)), style = MaterialTheme.typography.bodySmall)
            val durationMin = route.endTs?.let { (it - route.startTs) / 60_000 }
            Text(
                if (durationMin != null) "$durationMin min" else "In progress",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

