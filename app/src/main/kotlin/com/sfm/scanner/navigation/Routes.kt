package com.sfm.scanner.navigation

import com.sfm.scanner.feature.form.FormDestination
import com.sfm.scanner.feature.scan.PackagingDestination
import com.sfm.scanner.feature.scan.ScanDestination
import com.sfm.scanner.feature.selection.SelectionDestination
import com.sfm.scanner.feature.splash.SplashDestination
import com.sfm.scanner.feature.upload.UploadDestination

/**
 * Centralised route constants. Thin re-exports of each feature's `*Destination.route` so
 * tests that exercise route strings (e.g. [com.sfm.scanner.navigation.RoutesTest]) can
 * validate consistency between the app's view of the routes and the feature modules'
 * own destination objects.
 *
 * Resolves implementation_status §4 C-1: previously `FORM = "form"` here vs.
 * `FormDestination.route = "form/{selectionId}"` in the feature — those are now sourced
 * from the same place.
 */
object Routes {
    const val SPLASH = SplashDestination.route
    const val SELECTION = SelectionDestination.route
    const val FORM = FormDestination.route
    const val SCAN = ScanDestination.route
    const val PACKAGING = PackagingDestination.route
    const val UPLOAD = UploadDestination.route

    const val ARG_SELECTION_ID = FormDestination.ARG_SELECTION_ID
    const val ARG_FORM_DATA = ScanDestination.ARG_FORM_DATA
    const val ARG_ZIP_ARTIFACT = UploadDestination.ARG_ZIP_ARTIFACT
}
