package com.sfm.scanner.navigation

import com.sfm.scanner.feature.form.FormDestination
import com.sfm.scanner.feature.scan.PackagingDestination
import com.sfm.scanner.feature.scan.ScanDestination
import com.sfm.scanner.feature.selection.SelectionDestination
import com.sfm.scanner.feature.splash.SplashDestination
import com.sfm.scanner.feature.upload.UploadDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [Routes] stays in sync with each feature module's `*Destination.route`.
 * Resolves implementation_status §4 C-1 (Routes vs. FormDestination drift).
 */
class RoutesTest {

    @Test
    fun splash_route_matches_splash_destination() {
        assertEquals(SplashDestination.route, Routes.SPLASH)
        assertEquals("splash", Routes.SPLASH)
    }

    @Test
    fun selection_route_matches_selection_destination() {
        assertEquals(SelectionDestination.route, Routes.SELECTION)
        assertEquals("selection", Routes.SELECTION)
    }

    @Test
    fun form_route_matches_form_destination_with_selection_id_arg() {
        assertEquals(FormDestination.route, Routes.FORM)
        assertTrue(Routes.FORM.contains(Routes.ARG_SELECTION_ID))
        assertEquals("form/{selectionId}", Routes.FORM)
    }

    @Test
    fun scan_route_matches_scan_destination_with_form_data_arg() {
        assertEquals(ScanDestination.route, Routes.SCAN)
        assertTrue(Routes.SCAN.contains(Routes.ARG_FORM_DATA))
        assertEquals("scan/{formDataJson}", Routes.SCAN)
    }

    @Test
    fun packaging_route_matches_packaging_destination() {
        assertEquals(PackagingDestination.route, Routes.PACKAGING)
        assertEquals("packaging", Routes.PACKAGING)
    }

    @Test
    fun upload_route_matches_upload_destination_with_zip_artifact_arg() {
        assertEquals(UploadDestination.route, Routes.UPLOAD)
        assertTrue(Routes.UPLOAD.contains(Routes.ARG_ZIP_ARTIFACT))
        assertEquals("upload/{zipArtifactJson}", Routes.UPLOAD)
    }

    @Test
    fun arg_names_are_stable() {
        assertEquals("selectionId", Routes.ARG_SELECTION_ID)
        assertEquals("formDataJson", Routes.ARG_FORM_DATA)
        assertEquals("zipArtifactJson", Routes.ARG_ZIP_ARTIFACT)
    }

    @Test
    fun all_route_templates_are_unique() {
        val routes = listOf(
            Routes.SPLASH,
            Routes.SELECTION,
            Routes.FORM,
            Routes.SCAN,
            Routes.PACKAGING,
            Routes.UPLOAD,
        )
        assertEquals(routes.size, routes.toSet().size)
    }
}
