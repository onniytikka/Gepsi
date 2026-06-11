package com.gepsi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gepsi.ui.AllRoutesScreen
import com.gepsi.ui.ImportDialog
import com.gepsi.ui.MapScreen
import com.gepsi.ui.RouteDetailScreen
import com.gepsi.ui.RouteListScreen

class MainActivity : ComponentActivity() {

    private val pendingImport = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncoming(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val nav = rememberNavController()
                    NavHost(nav, startDestination = "map") {
                        composable("map") {
                            MapScreen(
                                onOpenRoutes = { nav.navigate("routes") }
                            )
                        }
                        composable("routes") {
                            RouteListScreen(
                                onOpenRoute = { id -> nav.navigate("routes/$id") },
                                onOpenAll = { nav.navigate("routes/all") },
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("routes/all") {
                            AllRoutesScreen(onBack = { nav.popBackStack() })
                        }
                        composable("routes/{routeId}") { entry ->
                            val id = entry.arguments?.getString("routeId")?.toLongOrNull() ?: return@composable
                            RouteDetailScreen(routeId = id, onBack = { nav.popBackStack() })
                        }
                    }

                    pendingImport.value?.let { uri ->
                        ImportDialog(uri = uri, onDone = { pendingImport.value = null })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            else -> null
        }
        if (uri != null) {
            pendingImport.value = uri
            intent?.action = null
        }
    }
}
