package com.sfm.scanner.feature.scan.domain.usecase

import com.sfm.scanner.data.ar.ARMeasurement
import com.sfm.scanner.data.ar.ArPoint
import com.sfm.scanner.data.ar.ArTrackingState
import com.sfm.scanner.data.ar.CameraPose
import com.sfm.scanner.data.ar.HitType
import com.sfm.scanner.data.camera.FrameRecord
import com.sfm.scanner.feature.scan.domain.model.FormDataSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.File

class StopScanUseCaseTest {

    private val useCase = StopScanUseCase()

    private val sampleFormData = FormDataSnapshot(
        initialSelection = "option_a",
        dropdownSelection = "type_a",
        size = 5,
        detail = "ABC",
        gt = null,
    )

    private val sampleSessionDir = File("/tmp/test-session")

    @Test
    fun `buildSession copies all primitive fields`() {
        val session = useCase.buildSession(
            sessionUuid = "uuid-1",
            formData = sampleFormData,
            frames = emptyList(),
            measurement = sampleMeasurement(),
            scanStartTimestampMs = 1234567890L,
            appVersion = "1.0.0",
            deviceInfo = "TestVendor TestModel (Android 14)",
            sessionDir = sampleSessionDir,
        )

        assertEquals("uuid-1", session.sessionUUID)
        assertEquals(sampleFormData, session.formData)
        assertEquals(1234567890L, session.scanStartTimestampMs)
        assertEquals("1.0.0", session.appVersion)
        assertEquals("TestVendor TestModel (Android 14)", session.deviceInfo)
        assertSame(sampleSessionDir, session.sessionDir)
    }

    @Test
    fun `buildSession preserves frames in order`() {
        val frames = listOf(
            frameRecord(1, "frame_000001.jpg"),
            frameRecord(2, "frame_000002.jpg"),
            frameRecord(3, "frame_000003.jpg"),
        )
        val session = useCase.buildSession(
            sessionUuid = "u",
            formData = sampleFormData,
            frames = frames,
            measurement = sampleMeasurement(),
            scanStartTimestampMs = 0L,
            appVersion = "1.0",
            deviceInfo = "x",
            sessionDir = sampleSessionDir,
        )
        assertEquals(frames, session.frames)
    }

    @Test
    fun `buildSession substitutes empty ARMeasurement when measurement is null`() {
        val session = useCase.buildSession(
            sessionUuid = "u",
            formData = sampleFormData,
            frames = emptyList(),
            measurement = null,
            scanStartTimestampMs = 0L,
            appVersion = "1.0",
            deviceInfo = "x",
            sessionDir = sampleSessionDir,
        )
        assertEquals(ArTrackingState.PAUSED, session.measurement.trackingState)
        assertEquals(HitType.NONE, session.measurement.hitType)
        assertNull(session.measurement.pointA)
        assertNull(session.measurement.pointB)
        assertNull(session.measurement.distanceMeters)
    }

    @Test
    fun `buildSession preserves complete ARMeasurement when provided`() {
        val measurement = ARMeasurement(
            pointA = ArPoint(0f, 0f, 0f, 100L),
            pointB = ArPoint(0.4f, 0f, 0f, 200L),
            distanceMeters = 0.4f,
            cameraPoseA = CameraPose(0f, 0f, 0f, 0f, 0f, 0f, 1f),
            cameraPoseB = CameraPose(0.4f, 0f, 0f, 0f, 0f, 0f, 1f),
            trackingState = ArTrackingState.TRACKING,
            hitType = HitType.DEPTH,
        )
        val session = useCase.buildSession(
            sessionUuid = "u",
            formData = sampleFormData,
            frames = emptyList(),
            measurement = measurement,
            scanStartTimestampMs = 0L,
            appVersion = "1.0",
            deviceInfo = "x",
            sessionDir = sampleSessionDir,
        )
        assertEquals(measurement, session.measurement)
        assertEquals(HitType.DEPTH, session.measurement.hitType)
        assertEquals(0.4f, session.measurement.distanceMeters!!, 1e-5f)
    }

    @Test
    fun `buildSession preserves gt list including empty list`() {
        val formWithGt = sampleFormData.copy(gt = listOf(1.0, 2.5, 3.7))
        val session = useCase.buildSession(
            sessionUuid = "u",
            formData = formWithGt,
            frames = emptyList(),
            measurement = sampleMeasurement(),
            scanStartTimestampMs = 0L,
            appVersion = "1.0",
            deviceInfo = "x",
            sessionDir = sampleSessionDir,
        )
        assertEquals(listOf(1.0, 2.5, 3.7), session.formData.gt)
    }

    private fun sampleMeasurement(): ARMeasurement = ARMeasurement(
        trackingState = ArTrackingState.TRACKING,
        hitType = HitType.NONE,
    )

    private fun frameRecord(index: Int, filename: String) = FrameRecord(
        index = index,
        filename = filename,
        absolutePath = "/tmp/test-session/$filename",
        timestampMs = 1000L * index,
    )
}
