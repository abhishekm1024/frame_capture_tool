package com.sfm.scanner.feature.scan.infra

import app.cash.turbine.test
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.data.camera.FrameRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FrameWriterTest {

    private lateinit var tempDir: File
    private lateinit var dispatchers: AppDispatchers
    private lateinit var writer: FrameWriter

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("framewriter-test").toFile()
        dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher(),
            main = UnconfinedTestDispatcher(),
        )
        writer = FrameWriter(tempDir, dispatchers)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `submit writes frame to disk and emits frameWritten`() = runTest {
        writer.start(this)
        writer.frameWritten.test {
            writer.submit(record(1, "frame_000001.jpg"), byteArrayOf(0x01, 0x02, 0x03))
            val emitted = awaitItem()
            assertEquals(1, emitted.index)
            assertEquals("frame_000001.jpg", emitted.filename)
            val written = File(tempDir, "frame_000001.jpg")
            assertTrue("File should exist on disk", written.exists())
            assertEquals(3, written.length())
            cancelAndIgnoreRemainingEvents()
        }
        writer.stopAndDrain()
    }

    @Test
    fun `frameWritten record carries the full absolute path`() = runTest {
        writer.start(this)
        writer.frameWritten.test {
            writer.submit(record(1, "frame_000001.jpg"), byteArrayOf(0x42))
            val emitted = awaitItem()
            val expected = File(tempDir, "frame_000001.jpg").absolutePath
            assertEquals(expected, emitted.absolutePath)
            cancelAndIgnoreRemainingEvents()
        }
        writer.stopAndDrain()
    }

    @Test
    fun `submittedCount increments regardless of drops`() = runTest {
        writer.start(this)
        repeat(10) { idx ->
            writer.submit(record(idx + 1, "frame_${"%06d".format(idx + 1)}.jpg"), byteArrayOf(idx.toByte()))
        }
        assertEquals(10, writer.submittedCount)
        writer.stopAndDrain()
    }

    @Test
    fun `stopAndDrain finishes pending writes`() = runTest {
        writer.start(this)
        repeat(3) { idx ->
            writer.submit(record(idx + 1, "frame_${"%06d".format(idx + 1)}.jpg"), byteArrayOf(idx.toByte()))
        }
        writer.stopAndDrain()
        // All submitted within capacity (3 ≤ QUEUE_CAPACITY=4) → all written
        assertEquals(3, writer.writtenCount)
    }

    @Test
    fun `start twice throws IllegalStateException`() = runTest {
        writer.start(this)
        try {
            writer.start(this)
            error("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already started") == true)
        }
        writer.stopAndDrain()
    }

    private fun record(index: Int, filename: String) = FrameRecord(
        index = index,
        filename = filename,
        absolutePath = filename,
        timestampMs = 1000L * index,
    )
}
