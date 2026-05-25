package com.sfm.scanner.feature.scan.domain.usecase

import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.core.storage.AppFileProvider
import com.sfm.scanner.core.storage.SessionDirectoryManager
import com.sfm.scanner.core.storage.StorageConstants
import com.sfm.scanner.core.storage.ZipBuilder
import com.sfm.scanner.feature.scan.domain.model.DetailsJson
import com.sfm.scanner.feature.scan.domain.model.PackagedSession
import com.sfm.scanner.feature.scan.domain.model.ScanSession
import com.sfm.scanner.feature.scan.domain.model.toJson
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Builds the upload ZIP from a completed [ScanSession].
 *
 * Workflow (functional_spec §6.1):
 *  1. Build details.json and measurements.json
 *  2. Compute ZIP filename: `{UUID}_{unixTimestampSeconds}.zip`
 *  3. Create ZIP in `filesDir/uploads/` via [ZipBuilder] (atomic write via .tmp + rename)
 *  4. Add JSON files + each frame JPEG (in ascending frame order)
 *  5. Delete session temp directory
 *  6. Return [PackagedSession] — JSON-shape-compatible with feature-upload's ZipArtifact
 *
 * R-16 mitigation: [ZipBuilder] writes atomically (`.tmp` then rename), so a process
 * kill during write never produces a half-written ZIP at the final filename.
 *
 * Runs entirely on [AppDispatchers.io].
 */
internal class PackageSessionUseCase @Inject constructor(
    private val zipBuilder: ZipBuilder,
    private val appFileProvider: AppFileProvider,
    private val sessionDirectoryManager: SessionDirectoryManager,
    private val dispatchers: AppDispatchers,
) {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = true
    }

    suspend fun execute(session: ScanSession): Result<PackagedSession> =
        withContext(dispatchers.io) {
            runCatching { buildZip(session) }
        }

    private fun buildZip(session: ScanSession): PackagedSession {
        val unixSecs = session.scanStartTimestampMs / 1000L
        val zipFilename = StorageConstants.ZIP_FILENAME_FORMAT.format(session.sessionUUID, unixSecs)
        val zipFile = File(appFileProvider.uploadsDir, zipFilename)

        val detailsBytes = serializeDetails(session)
        val measurementsBytes = serializeMeasurements(session)

        zipBuilder.create(zipFile) {
            addBytes("details.json", detailsBytes)
            addBytes("measurements.json", measurementsBytes)
            // Frames added in ascending index order per data_contracts §3.3
            for (frame in session.frames.sortedBy { it.index }) {
                addFile("frames/${frame.filename}", File(frame.absolutePath))
            }
        }

        // Clean up session temp directory (frames are now in the ZIP)
        sessionDirectoryManager.deleteSessionDir(session.sessionUUID)

        return PackagedSession(
            sessionUUID = session.sessionUUID,
            zipFilename = zipFilename,
            absolutePath = zipFile.absolutePath,
            fileSizeBytes = zipFile.length(),
        )
    }

    private fun serializeDetails(session: ScanSession): ByteArray {
        // Empty gt is treated as null per data_contracts §2.1
        val gt = session.formData.gt?.takeIf { it.isNotEmpty() }
        val details = DetailsJson(
            initialSelection = session.formData.initialSelection,
            dropdownSelection = session.formData.dropdownSelection,
            size = session.formData.size,
            detail = session.formData.detail,
            gt = gt,
            scanTimestamp = formatIsoUtc(session.scanStartTimestampMs),
            deviceInfo = session.deviceInfo,
            appVersion = session.appVersion,
        )
        return json.encodeToString(details).toByteArray(Charsets.UTF_8)
    }

    private fun serializeMeasurements(session: ScanSession): ByteArray {
        val measurements = session.measurement.toJson()
        return json.encodeToString(measurements).toByteArray(Charsets.UTF_8)
    }

    private fun formatIsoUtc(epochMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(epochMs))
    }
}
