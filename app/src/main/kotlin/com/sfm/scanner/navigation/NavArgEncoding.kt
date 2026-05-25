package com.sfm.scanner.navigation

import com.sfm.scanner.feature.form.FormData
import com.sfm.scanner.feature.upload.ZipArtifact
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * URL-encoded JSON helpers for nav-graph arguments.
 *
 * The Android Navigation library carries string arguments verbatim in the route, which means
 * the encoded JSON must not contain reserved URL characters (`/`, `?`, `#`, `=`, etc.). We
 * round-trip via [URLEncoder] (UTF-8) so that all separators are percent-encoded.
 *
 * Decoding happens inside each feature's ViewModel via `SavedStateHandle` plus `URLDecoder` —
 * the consumer side already exists in `FormViewModel`, `ScanViewModel`, `UploadViewModel`.
 *
 * Per data_contracts.md §5.1 and §5.2 the nav-arg shapes are:
 *  - `formDataJson` → [FormData] (consumed by `ScanViewModel` as `FormDataSnapshot`)
 *  - `zipArtifactJson` → [ZipArtifact] (consumed by `UploadViewModel`)
 */
internal object NavArgEncoding {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
    }

    /** Encodes [FormData] to the URL-safe JSON string used as the `formDataJson` nav arg. */
    fun encodeFormData(formData: FormData): String {
        val raw = json.encodeToString(FormData.serializer(), formData)
        return URLEncoder.encode(raw, Charsets.UTF_8.name())
    }

    /**
     * Builds a `zipArtifactJson` payload that represents a *failed* packaging step.
     * The absolutePath is empty so [com.sfm.scanner.feature.upload.UploadZipUseCase]
     * short-circuits on the missing-file check and emits `UploadStage.Failed`
     * (per screen_specs §6.5: failed packaging surfaces the Failed UI variant on
     * UploadScreen).
     *
     * [errorMessage] is encoded into [ZipArtifact.zipFilename] as the surface that
     * UploadScreen displays in its Failed variant.
     */
    fun encodeFailedArtifact(errorMessage: String): String {
        val artifact = ZipArtifact(
            sessionUUID = "",
            zipFilename = errorMessage,
            absolutePath = "",
            fileSizeBytes = 0L,
        )
        val raw = json.encodeToString(ZipArtifact.serializer(), artifact)
        return URLEncoder.encode(raw, Charsets.UTF_8.name())
    }
}
