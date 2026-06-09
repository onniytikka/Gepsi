package com.gepsi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gepsi.audio.VoiceRecorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    onDismiss: () -> Unit,
    onSave: (text: String?, voicePath: String?) -> Unit,
) {
    val ctx = LocalContext.current
    val recorder = remember { VoiceRecorder(ctx) }
    var text by remember { mutableStateOf("") }
    var voicePath by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(Unit) {
        onDispose { if (recording) recorder.cancel() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New note", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Text (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        if (!recording) {
                            val file = runCatching { recorder.start() }.getOrNull()
                            if (file != null) {
                                voicePath = file.absolutePath
                                recording = true
                            }
                        } else {
                            recorder.stop()
                            recording = false
                        }
                    }
                ) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                    Text(if (recording) "  Stop" else "  Record")
                }
                if (voicePath != null && !recording) {
                    Text("Voice memo saved", modifier = Modifier.padding(top = 12.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    if (recording) recorder.cancel()
                    onDismiss()
                }) { Text("Cancel") }
                Button(onClick = {
                    if (recording) recorder.stop()
                    onSave(text.ifBlank { null }, voicePath)
                }) { Text("Save") }
            }
        }
    }
}
