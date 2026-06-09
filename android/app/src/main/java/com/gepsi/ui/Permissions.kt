package com.gepsi.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private fun foregroundPerms(): List<String> {
    val base = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base += Manifest.permission.POST_NOTIFICATIONS
    }
    return base
}

private fun backgroundPerm(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    else null

fun hasForegroundPerms(ctx: Context): Boolean =
    foregroundPerms().all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

fun hasBackgroundPerm(ctx: Context): Boolean {
    val p = backgroundPerm() ?: return true
    return ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun rememberPermissionRequester(onAllGranted: () -> Unit = {}): PermissionRequester {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasForegroundPerms(ctx)) }
    var backgroundGranted by remember { mutableStateOf(hasBackgroundPerm(ctx)) }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        backgroundGranted = ok || backgroundPerm() == null
        if (granted && backgroundGranted) onAllGranted()
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
        if (granted) {
            val bg = backgroundPerm()
            if (bg != null && !hasBackgroundPerm(ctx)) {
                backgroundLauncher.launch(bg)
            } else {
                backgroundGranted = true
                onAllGranted()
            }
        }
    }

    return remember {
        PermissionRequester(
            request = {
                if (!hasForegroundPerms(ctx)) {
                    foregroundLauncher.launch(foregroundPerms().toTypedArray())
                } else {
                    val bg = backgroundPerm()
                    if (bg != null && !hasBackgroundPerm(ctx)) backgroundLauncher.launch(bg)
                    else onAllGranted()
                }
            },
            isGranted = { granted && backgroundGranted },
        )
    }
}

class PermissionRequester(
    val request: () -> Unit,
    val isGranted: () -> Boolean,
)

@Composable
fun ensureFirstRunPermissions(requester: PermissionRequester) {
    LaunchedEffect(Unit) {
        if (!requester.isGranted()) requester.request()
    }
}
