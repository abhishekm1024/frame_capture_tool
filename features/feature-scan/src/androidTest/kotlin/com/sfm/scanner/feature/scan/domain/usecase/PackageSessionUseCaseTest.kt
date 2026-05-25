package com.sfm.scanner.feature.scan.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.core.storage.AppFileProvider
import com.sfm.scanner.core.storage.SessionDirectoryManager
import com.sfm.scanner.core.storage.ZipBuilder
import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArPoint
import com.sfm.scanner.data.ar.ArTrackingState
import com.sfm.scanner.data.ar.CameraPose
import com.sfm.scanner.data.ar.HitType
import com.sfm.scanner.data.camera.FrameRecord
import com.sfm.scanner.feature.scan.domain.model.FormDataSnapshot
import com.sfm.scanner.feature.scan.domain.model.ScanSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class PackageSessionUseCaseTest {

    private lateinit var context: Context
    private lateinit var useCase: PackageSessionUseCase
    private lateinit var appFileProvider: AppFileProvider
    private lateinit var sessionDirManager: SessionDirectoryManager
    private lateinit var sessionUuid: String
    private lateinit var sessionDir: File
    private lateinit var frames: List<FrameRecord>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appFileProvider = AppFileProvider(context)
        sessionDirManager = SessionDirectoryManager(appFileProvider)
        useCase = PackageSessionUseCase(
            zipBuilder = ZipBuilder(),
            appFileProvider = appFileProvider,
            sessionDirectoryManager = sessionDirManager,
            dispatchers = AppDispatchers(
                io = Dispatchers.Unconfined,
                default = Dispatchers.Unconfined,
                main = Dispatchers.Unconfined,
            ),
        )
        sessionUuid = UUID.randomUUID().toString()
        sessionDir = sessionDirManager.createSessionDir(sessionUuid)
        // Create 3 dummy frame files
        frames = (1..3).map { idx ->
            val filename = "frame_%06d.jpg".format(idx)
            val file = File(sessionDir, filename).apply {
                writeBytes(byteArrayOf(idx.toByte(), 0xFF.toByte(), 0xD8.toByte()))
            }
            FrameRecord(
                index = idx,
                filename = filename,
                absolutePath = file.absolutePath,
                timestampMs = 1_000_000L + idx,
            )
        }
    }

    @After
    fun tearDown() {
        sessionDirManager.deleteSessionDir(sessionUuid)
        // Clean up any uploaded zips for this test run
        appFileProvider.uploadsDir.listFiles { _, name -> name.startsWith(sessionUuid) }
            ?.forEach { it.delete() }
    }

    @Test
    fun execute_produces_zip_with_correct_filename() = runTest {
        val session = buildSession()
        val result = useCase.execute(session)
        assertTrue("execute should succeed", result.isSuccess)
        val packaged = result.getOrNull()!!
        assertTrue(
            "filename matches {UUID}_{unixSecs}.zip",
            packaged.zipFilename.matches(Regex("""^${sessionUuid}_\d+\.zip$""")),
        )
        assertTrue("ZIP file exists on disk", File(packaged.absolutePath).exists())
        assertTrue("File size > 0", packaged.fileSizeBytes > 0)
    }

    @Test
    fun execute_zip_contains_details_measurements_and_all_frames() = runTest {
        val session = buildSession()
        val result = useCase.execute(session)
        val packaged = result.getOrNull()!!

        ZipFile(packaged.absolutePath).use { zip ->
            val entryNames = zip.entries().toList().map { it.name }.toSet()
            assertTrue("details.json present", "details.json" in entryNames)
            assertTrue("measurements.json present", "measurements.json" in entryNames)
            assertTrue("frame 1 present", "frames/frame_000001.jpg" in entryNames)
            assertTrue("frame 2 present", "frames/frame_000002.jpg" in entryNames)
            assertTrue("frame 3 present", "frames/frame_000003.jpg" in entryNames)
        }
    }

    @Test
    fun execute_details_json_has_expected_fields() = runTest {
        val session = buildSession()
        val result = useCase.execute(session)
        val packaged = result.getOrNull()!!

        ZipFile(packaged.absolutePath).use { zip ->
            val detailsEntry = zip.getEntry("details.json")!!
            val detailsJson = zip.getInputStream(detailsEntry).bufferedReader().readText()
            assertTrue("contains initialSelection", detailsJson.contains("\"initialSelection\""))
            assertTrue("contains dropdownSelection", detailsJson.contains("\"dropdownSelection\""))
            assertTrue("contains size", detailsJson.contains("\"size\":42"))
            assertTrue("contains detail", detailsJson.contains("\"detail\":\"TEST123\""))
            assertTrue("contains scanTimestamp", detailsJson.contains("\"scanTimestamp\""))
            assertTrue("contains deviceInfo", detailsJson.contains("\"deviceInfo\""))
            assertTrue("contains appVersion", detailsJson.contains("\"appVersion\":\"1.0.0\""))
        }
    }

    @Test
    fun execute_measurements_json_contains_pointA_and_distance() = runTest {
        val session = buildSession(
            measurement = ARMeasurement(
                pointA = ArPoint(0.1f, 0.2f, 1.0f, 100L),
                pointB = ArPoint(0.4f, 0.2f, 1.0f, 200L),
                distanceMeters = 0.3f,
                cameraPoseA = CameraPose(0f, 0f, 0f, 0f, 0f, 0f, 1f),
                cameraPoseB = CameraPose(0.3f, 0f, 0f, 0f, 0f, 0f, 1f),
                trackingState = ArTrackingState.TRACKING,
                hitType = HitType.DEPTH,
            ),
        )
        val result = useCase.execute(session)
        val packaged = result.getOrNull()!!

        ZipFile(packaged.absolutePath).use { zip ->
            val entry = zip.getEntry("measurements.json")!!
            val json = zip.getInputStream(entry).bufferedReader().readText()
            assertTrue("contains pointA.x", json.contains("\"x\":0.1"))
            assertTrue("contains distanceMeters", json.contains("\"distanceMeters\":0.3"))
            assertTrue("trackingState is TRACKING", json.contains("\"trackingState\":\"TRACKING\""))
            assertTrue("hitType is DEPTH", json.contains("\"hitType\":\"DEPTH\""))
        }
    }

    @Test
    fun execute_deletes_session_temp_directory_on_success() = runTest {
        assertTrue("session dir exists before packaging", sessionDir.exists())
        val session = buildSession()
        useCase.execute(session).getOrNull()
        assertTrue("session dir deleted after packaging", !appFileProvider.sessionDir(sessionUuid).exists())
    }

    private fun buildSession(
        measurement: ARMeasurement = ARMeasurement(
            trackingState = ArTrackingState.TRACKING,
            hitType = HitType.NONE,
        ),
    ) = ScanSession(
        sessionUUID = sessionUuid,
        formData = FormDataSnapshot(
            initialSelection = "option_a",
            dropdownSelection = "type_a",
            size = 42,
            detail = "TEST123",
            gt = listOf(1.0, 2.5),
        ),
        frames = frames,
        measurement = measurement,
        scanStartTimestampMs = 1_700_000_000_000L,
        appVersion = "1.0.0",
        deviceInfo = "TestVendor TestModel (Android 14)",
        sessionDir = sessionDir,
    )
}
