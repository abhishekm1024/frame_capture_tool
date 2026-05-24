package com.sfm.scanner.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Root navigation graph for the app.
 *
 * Each composable destination is a scaffold placeholder replaced milestone by milestone:
 *   M5-A → SplashScreen
 *   M5-B → SelectionScreen
 *   M5-C → FormScreen
 *   M6   → ScanScreen + PackagingScreen
 *   M7   → UploadScreen
 *
 * Navigation actions (navController.navigate calls) are wired in M8 once all
 * feature modules are implemented and their navigation lambdas are defined.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(route = Routes.SPLASH) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(route = Routes.SELECTION) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(route = Routes.FORM) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(
            route = Routes.SCAN,
            arguments = listOf(
                navArgument(Routes.ARG_FORM_DATA) { type = NavType.StringType },
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(route = Routes.PACKAGING) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(
            route = Routes.UPLOAD,
            arguments = listOf(
                navArgument(Routes.ARG_ZIP_ARTIFACT) { type = NavType.StringType },
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
