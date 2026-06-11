package com.gepsi.ui

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gepsi.GepsiApp
import com.gepsi.exchange.ImportPreview
import com.gepsi.exchange.ImportResult
import com.gepsi.exchange.InvalidPackageException
import com.gepsi.exchange.PackageImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportState {
    data object Loading : ImportState
    data class Preview(val preview: ImportPreview) : ImportState
    data object Importing : ImportState
    data class Done(val result: ImportResult) : ImportState
    data class Failed(val message: String) : ImportState
}

class ImportViewModel(app: Application, uri: Uri) : AndroidViewModel(app) {
    private val importer = PackageImporter(app, GepsiApp.get().repository)

    private val _state = MutableStateFlow<ImportState>(ImportState.Loading)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = runCatching { ImportState.Preview(importer.preview(uri)) }
                .getOrElse { ImportState.Failed(messageFor(it)) }
        }
    }

    fun confirm() {
        val preview = (_state.value as? ImportState.Preview)?.preview ?: return
        _state.value = ImportState.Importing
        viewModelScope.launch {
            _state.value = runCatching { ImportState.Done(importer.commit(preview)) }
                .getOrElse { ImportState.Failed(messageFor(it)) }
        }
    }

    private fun messageFor(e: Throwable): String =
        (e as? InvalidPackageException)?.message ?: "Import failed"
}

@Composable
fun ImportDialog(uri: Uri, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ImportViewModel = viewModel(
        key = uri.toString(),
        factory = viewModelFactory {
            initializer { ImportViewModel(app, uri) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { if (state !is ImportState.Importing) onDone() },
        title = { Text("Import Gepsi data") },
        text = {
            when (val s = state) {
                ImportState.Loading -> ProgressRow("Reading package…")
                ImportState.Importing -> ProgressRow("Importing…")
                is ImportState.Preview -> {
                    val p = s.preview
                    val parts = buildString {
                        append("Import ${p.newRoutes.size} route(s), ${p.pointCount} points, ")
                        append("${p.noteCount} note(s) (${p.voiceCount} voice memo(s)).")
                        if (p.duplicateRouteNames.isNotEmpty()) {
                            append("\n\n${p.duplicateRouteNames.size} route(s) skipped — already imported.")
                        }
                        if (p.newRoutes.isEmpty()) {
                            append("\n\nNothing new to import.")
                        }
                    }
                    Text(parts)
                }
                is ImportState.Done -> Text(
                    "Imported ${s.result.routesImported} route(s), " +
                        "${s.result.pointsImported} points, ${s.result.notesImported} note(s)."
                )
                is ImportState.Failed -> Text(s.message)
            }
        },
        confirmButton = {
            when (val s = state) {
                is ImportState.Preview ->
                    if (s.preview.newRoutes.isNotEmpty()) {
                        Button(onClick = { vm.confirm() }) { Text("Import") }
                    } else {
                        TextButton(onClick = onDone) { Text("Close") }
                    }
                is ImportState.Done, is ImportState.Failed ->
                    TextButton(onClick = onDone) { Text("Close") }
                else -> {}
            }
        },
        dismissButton = {
            if (state is ImportState.Preview) {
                TextButton(onClick = onDone) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ProgressRow(label: String) {
    Row {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}
