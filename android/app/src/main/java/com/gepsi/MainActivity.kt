package com.gepsi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gepsi.ui.MapScreen
import com.gepsi.ui.RouteDetailScreen
import com.gepsi.ui.RouteListScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                onBack = { nav.popBackStack() }
                            )
                        }
                        composable("routes/{routeId}") { entry ->
                            val id = entry.arguments?.getString("routeId")?.toLongOrNull() ?: return@composable
                            RouteDetailScreen(routeId = id, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
