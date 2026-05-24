package com.sfm.scanner.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    @Test
    fun splash_route_is_correct() {
        assertEquals("splash", Routes.SPLASH)
    }

    @Test
    fun selection_route_is_correct() {
        assertEquals("selection", Routes.SELECTION)
    }

    @Test
    fun form_route_is_correct() {
        assertEquals("form", Routes.FORM)
    }

    @Test
    fun scan_route_contains_form_data_arg() {
        assertTrue(Routes.SCAN.contains(Routes.ARG_FORM_DATA))
        assertEquals("scan/{formDataJson}", Routes.SCAN)
    }

    @Test
    fun packaging_route_is_correct() {
        assertEquals("packaging", Routes.PACKAGING)
    }

    @Test
    fun upload_route_contains_zip_artifact_arg() {
        assertTrue(Routes.UPLOAD.contains(Routes.ARG_ZIP_ARTIFACT))
        assertEquals("upload/{zipArtifactJson}", Routes.UPLOAD)
    }

    @Test
    fun arg_names_are_stable() {
        assertEquals("formDataJson", Routes.ARG_FORM_DATA)
        assertEquals("zipArtifactJson", Routes.ARG_ZIP_ARTIFACT)
    }

    @Test
    fun all_routes_are_unique() {
        val routes = listOf(
            Routes.SPLASH,
            Routes.SELECTION,
            Routes.FORM,
            Routes.PACKAGING,
        )
        assertEquals(routes.size, routes.toSet().size)
    }
}
