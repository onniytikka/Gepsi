package com.gepsi.update

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Checks for a newer published APK once per app start and offers to download it.
 * Installing over the current version keeps all routes and notes.
 */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { update = UpdateChecker.check() }

    val info = update ?: return
    if (dismissed) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text("Update available") },
        text = {
            Text(
                "Gepsi ${info.versionName} is out. Download it and open the file to " +
                    "install — your routes and notes are kept."
            )
        },
        confirmButton = {
            Button(onClick = {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)))
                dismissed = true
            }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = { dismissed = true }) { Text("Later") }
        },
    )
}
