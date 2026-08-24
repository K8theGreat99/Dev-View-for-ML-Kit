package com.k8thegreat.devview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.k8thegreat.devview.ui.GalleryViewModel
import com.k8thegreat.devview.ui.about.AboutScreen
import com.k8thegreat.devview.ui.detail.DetailScreen
import com.k8thegreat.devview.ui.gallery.GalleryScreen
import com.k8thegreat.devview.ui.theme.DevViewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevViewTheme { DevViewApp() }
        }
    }
}

@Composable
private fun DevViewApp() {
    val navController = rememberNavController()
    // One ViewModel for the whole graph: the detail screen reads the same repository
    // the gallery does, so a second instance would only duplicate database handles.
    val viewModel: GalleryViewModel = viewModel()

    NavHost(navController = navController, startDestination = "gallery") {
        composable("gallery") {
            GalleryScreen(
                viewModel = viewModel,
                onOpenSample = { id -> navController.navigate("sample/$id") },
                onOpenAbout = { navController.navigate("about") },
            )
        }
        composable(
            route = "sample/{sampleId}",
            arguments = listOf(navArgument("sampleId") { type = NavType.StringType }),
        ) { entry ->
            DetailScreen(
                sampleId = entry.arguments?.getString("sampleId").orEmpty(),
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
