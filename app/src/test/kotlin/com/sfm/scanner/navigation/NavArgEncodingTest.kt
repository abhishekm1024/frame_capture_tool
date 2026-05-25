package com.sfm.scanner.navigation

import com.sfm.scanner.feature.form.FormData
import com.sfm.scanner.feature.upload.ZipArtifact
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/**
 * Verifies the nav-arg JSON contracts that flow between feature modules.
 *
 * Two contracts are covered:
 *  1. `formDataJson` round-trip: `FormData` encoded by [NavArgEncoding.encodeFormData]
 *     must be decodable into the JSON-shape-compatible `FormDataSnapshot` consumed by
 *     `ScanViewModel`. This validates implementation_status §6 V (the deviation E-1
 *     contract).
 *  2. `zipArtifactJson` failure payload: a synthetic [ZipArtifact] with an empty
 *     `absolutePath` must encode/decode cleanly so `UploadZipUseCase` short-circuits
 *     to `UploadStage.Failed`.
 *
 * `FormDataSnapshot` lives inside `:features:feature-scan` and is `internal`, so we
 * validate the JSON shape structurally (key names, value types) here instead of
 * importing the class.
 */
class NavArgEncodingTest {

    private val json = Json { encodeDefaults = true; explicitNulls = true }

    @Test
    fun encodeFormData_produces_url_safe_string_with_no_unescaped_slashes() {
        val data = FormData(
            initialSelection = "option_a",
            dropdownSelection = "type_a",
            size = 5,
            detail = "ABC",
            gt = listOf(1.0, 2.0, 0.5),
        )
        val encoded = NavArgEncoding.encodeFormData(data)
        // URL-encoded JSON: braces, colons, quotes must all be percent-encoded so the
        // string survives being embedded into a NavController route template like
        // `scan/{formDataJson}` without breaking path-segment parsing.
        assertFalse("must not contain raw '/'", encoded.contains("/"))
        assertFalse("must not contain raw '{'", encoded.contains("{"))
        assertFalse("must not contain raw '}'", encoded.contains("}"))
        assertFalse("must not contain raw '\"'", encoded.contains("\""))
    }

    @Test
    fun encodeFormData_round_trips_via_url_decode_and_kotlinx_json() {
        val original = FormData(
            initialSelection = "option_b",
            dropdownSelection = "type_b",
            size = 99,
            detail = "edge case 123",
            gt = listOf(0.0, 12.5, -3.4),
        )
        val encoded = NavArgEncoding.encodeFormData(original)
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        val parsed = json.decodeFromString(FormData.serializer(), decoded)
        assertEquals(original, parsed)
    }

    @Test
    fun encodeFormData_with_null_gt_round_trips_to_null() {
        val original = FormData(
            initialSelection = "option_a",
            dropdownSelection = "type_c",
            size = 1,
            detail = "X",
            gt = null,
        )
        val encoded = NavArgEncoding.encodeFormData(original)
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        val parsed = json.decodeFromString(FormData.serializer(), decoded)
        assertEquals(null, parsed.gt)
    }

    @Test
    fun encodeFormData_json_shape_is_compatible_with_feature_scan_FormDataSnapshot() {
        // FormDataSnapshot (feature-scan) is the mirror type per design deviation E-1
        // (architecture §12 forbids cross-feature deps). We validate JSON shape here
        // by parsing back into the same `FormData` serializer — same field names and
        // types means feature-scan's `Json.decodeFromString(FormDataSnapshot.serializer(), …)`
        // will succeed.
        val original = FormData(
            initialSelection = "option_a",
            dropdownSelection = "type_a",
            size = 7,
            detail = "DETAIL",
            gt = listOf(1.5),
        )
        val encoded = NavArgEncoding.encodeFormData(original)
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        // Field-name assertions — these are the keys ScanViewModel.decodeFormData reads.
        assertTrue(decoded.contains("\"initialSelection\""))
        assertTrue(decoded.contains("\"dropdownSelection\""))
        assertTrue(decoded.contains("\"size\""))
        assertTrue(decoded.contains("\"detail\""))
        assertTrue(decoded.contains("\"gt\""))
    }

    @Test
    fun encodeFailedArtifact_produces_zipArtifact_with_empty_absolutePath() {
        val encoded = NavArgEncoding.encodeFailedArtifact("Out of disk space")
        val decoded = URLDecoder.decode(encoded, Charsets.UTF_8.name())
        val artifact = json.decodeFromString(ZipArtifact.serializer(), decoded)
        // absolutePath="" makes UploadZipUseCase's File.exists() check fail → Failed stage.
        assertEquals("", artifact.absolutePath)
        // sessionUUID empty so the queue retry path doesn't accidentally pick this up.
        assertEquals("", artifact.sessionUUID)
        // fileSizeBytes=0 signals nothing to upload.
        assertEquals(0L, artifact.fileSizeBytes)
        // Error message is carried in zipFilename for display.
        assertEquals("Out of disk space", artifact.zipFilename)
    }

    @Test
    fun encodeFailedArtifact_is_unique_per_error_message() {
        val a = NavArgEncoding.encodeFailedArtifact("Error A")
        val b = NavArgEncoding.encodeFailedArtifact("Error B")
        assertNotEquals(a, b)
    }
}
