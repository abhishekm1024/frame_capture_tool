package com.sfm.scanner.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sfm.scanner.feature.form.FormDestination
import com.sfm.scanner.feature.form.FormScreen
import com.sfm.scanner.feature.scan.PackagingDestination
import com.sfm.scanner.feature.scan.PackagingScreen
import com.sfm.scanner.feature.scan.ScanDestination
import com.sfm.scanner.feature.scan.ScanScreen
import com.sfm.scanner.feature.selection.SelectionDestination
import com.sfm.scanner.feature.selection.SelectionScreen
import com.sfm.scanner.feature.splash.SplashDestination
import com.sfm.scanner.feature.splash.SplashScreen
import com.sfm.scanner.feature.upload.UploadDestination
import com.sfm.scanner.feature.upload.UploadScreen

/**
 * Root navigation graph.
 *
 * Flow per architecture.md §4 / screen_specs §7:
 *
 *   Splash → Selection → Form → Scan → Packaging → Upload
 *                 ↑                                    │
 *                 └────────── "Start New Scan" ────────┘
 *
 * Back-stack rules:
 *  - Splash is popped on transition to Selection (`popUpTo SplashDestination inclusive`).
 *  - Scan, Packaging, Upload block system back via `BackHandler` inside each screen
 *    (TD-15 / screen_specs §6.5 / §7.8).
 *  - "Start New Scan" from Upload pops back to Selection (`popUpTo SelectionDestination
 *    inclusive=false`) and clears Form/Scan/Packaging/Upload from the stack so the user
 *    cannot back-navigate into a stale session.
 *
 * Nav-arg encoding:
 *  - `formDataJson` (Scan): URL-encoded JSON of `FormData` (data_contracts §5.1).
 *  - `zipArtifactJson` (Upload): already URL-encoded by `PackagingViewModel`
 *    (data_contracts §5.2).
 *
 * [onRequestArInstall] is invoked when ARCore is supported but not installed; the
 * Activity calls `ArCoreApk.requestInstall(...)` so the user is sent to Play Store.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onRequestArInstall: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = SplashDestination.route,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(route = SplashDestination.route) {
            SplashScreen(
                onNavigateToSelection = {
                    navController.navigate(SelectionDestination.route) {
                        popUpTo(SplashDestination.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = SelectionDestination.route) {
            SelectionScreen(
                onNavigateToForm = { selectionId ->
                    navController.navigate(FormDestination.createRoute(selectionId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = FormDestination.route,
            arguments = listOf(
                navArgument(FormDestination.ARG_SELECTION_ID) { type = NavType.StringType },
            ),
        ) {
            FormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScan = { formData ->
                    val encoded = NavArgEncoding.encodeFormData(formData)
                    navController.navigate(ScanDestination.createRoute(encoded)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = ScanDestination.route,
            arguments = listOf(
                navArgument(ScanDestination.ARG_FORM_DATA) { type = NavType.StringType },
            ),
        ) {
            ScanScreen(
                onNavigateToPackaging = {
                    navController.navigate(PackagingDestination.route) {
                        popUpTo(ScanDestination.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequestArInstall = onRequestArInstall,
            )
        }

        composable(route = PackagingDestination.route) {
            PackagingScreen(
                onNavigateToUpload = { zipArtifactJson ->
                    navController.navigate(UploadDestination.createRoute(zipArtifactJson)) {
                        popUpTo(PackagingDestination.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToUploadWithError = { errorMessage ->
                    // Per screen_specs §6.5: failed packaging routes to Upload with a
                    // synthetic ZipArtifact whose absolutePath is empty, so UploadScreen
                    // surfaces the Failed UI variant via UploadZipUseCase's missing-file
                    // branch.
                    val encoded = NavArgEncoding.encodeFailedArtifact(errorMessage)
                    navController.navigate(UploadDestination.createRoute(encoded)) {
                        popUpTo(PackagingDestination.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = UploadDestination.route,
            arguments = listOf(
                navArgument(UploadDestination.ARG_ZIP_ARTIFACT) { type = NavType.StringType },
            ),
        ) {
            UploadScreen(
                onNavigateToSelection = {
                    navController.navigate(SelectionDestination.route) {
                        popUpTo(SelectionDestination.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
