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
 * Verifies end-to-end module integration at the navigation level: each feature
 * module's destination/arg consts match the values [AppNavHost] passes through
 * URL-encoded JSON nav args. If a feature module renames a route or an arg, this
 * test (and the consumer ViewModels) will catch it at build time / unit-test time
 * — no need to launch a device to validate the graph wiring.
 */
class AppNavHostRouteTest {

    @Test
    fun form_createRoute_substitutes_selection_id_into_form_destination_route() {
        val route = FormDestination.createRoute("option_a")
        assertEquals("form/option_a", route)
        // The substituted route must be a structural match of the parameterised route.
        assertTrue(
            "createRoute must not contain the {arg} placeholder",
            !route.contains("{${FormDestination.ARG_SELECTION_ID}}"),
        )
    }

    @Test
    fun scan_createRoute_substitutes_form_data_into_scan_destination_route() {
        val route = ScanDestination.createRoute("encoded%20json")
        assertEquals("scan/encoded%20json", route)
        assertTrue(!route.contains("{${ScanDestination.ARG_FORM_DATA}}"))
    }

    @Test
    fun upload_createRoute_substitutes_zip_artifact_into_upload_destination_route() {
        val route = UploadDestination.createRoute("encoded%20artifact")
        assertEquals("upload/encoded%20artifact", route)
        assertTrue(!route.contains("{${UploadDestination.ARG_ZIP_ARTIFACT}}"))
    }

    @Test
    fun form_arg_name_matches_routes_alias() {
        assertEquals(FormDestination.ARG_SELECTION_ID, Routes.ARG_SELECTION_ID)
    }

    @Test
    fun scan_arg_name_matches_routes_alias() {
        assertEquals(ScanDestination.ARG_FORM_DATA, Routes.ARG_FORM_DATA)
    }

    @Test
    fun upload_arg_name_matches_routes_alias() {
        assertEquals(UploadDestination.ARG_ZIP_ARTIFACT, Routes.ARG_ZIP_ARTIFACT)
    }

    @Test
    fun packaging_route_has_no_args() {
        // PackagingScreen does not take nav args — ScanSession is transferred via
        // ScanSessionHolder per implementation_status §6 X (deviation acknowledged).
        assertTrue(!PackagingDestination.route.contains("{"))
        assertTrue(!PackagingDestination.route.contains("}"))
    }

    @Test
    fun splash_and_selection_routes_are_static() {
        assertTrue(!SplashDestination.route.contains("{"))
        assertTrue(!SelectionDestination.route.contains("{"))
    }
}
