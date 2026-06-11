package com.gepsi.ui

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gepsi.GepsiApp
import com.gepsi.data.Note

@Composable
fun NoteDialog(note: Note, onDismiss: () -> Unit) {
    val controller = GepsiApp.get().audioPlayer
    val activity = LocalContext.current as? Activity

    // Stop playback when the dialog truly goes away, but not when the
    // activity is being recreated for a configuration change (rotation).
    DisposableEffect(Unit) {
        onDispose {
            if (activity?.isChangingConfigurations != true) controller.stop()
        }
    }

    val close = {
        controller.stop()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("Note") },
        text = {
            Column {
                Text(note.text ?: if (note.voicePath != null) "Voice memo" else "(empty)")
                if (note.voicePath != null) {
                    VoicePlayer(voicePath = note.voicePath)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = close) { Text("Close") }
        },
    )
}

@Composable
private fun VoicePlayer(voicePath: String) {
    val controller = GepsiApp.get().audioPlayer
    val state by controller.state.collectAsStateWithLifecycle()

    LaunchedEffect(voicePath) { controller.prepare(voicePath) }

    if (state.error) {
        Text(
            "Audio unavailable",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        return
    }

    var dragMs by remember { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(1)
    val shownMs = dragMs?.toInt() ?: state.positionMs

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        IconButton(onClick = { controller.togglePlayPause() }) {
            Icon(
                if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.playing) "Pause" else "Play",
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = shownMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                onValueChange = { dragMs = it },
                onValueChangeFinished = {
                    dragMs?.let { controller.seekTo(it.toInt()) }
                    dragMs = null
                },
                valueRange = 0f..durationMs.toFloat(),
            )
            Text(
                "${formatMs(shownMs)} / ${formatMs(state.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
