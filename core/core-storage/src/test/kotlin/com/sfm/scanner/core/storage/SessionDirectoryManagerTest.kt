package com.sfm.scanner.core.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SessionDirectoryManagerTest {

    private lateinit var tempRoot: File
    private lateinit var manager: SessionDirectoryManager

    @Before
    fun setUp() {
        tempRoot = Files.createTempDirectory("session_mgr_test").toFile()

        val mockContext = mockk<Context>()
        every { mockContext.cacheDir } returns tempRoot
        every { mockContext.filesDir } returns tempRoot

        val provider = AppFileProvider(mockContext)
        manager = SessionDirectoryManager(provider)
    }

    @After
    fun tearDown() {
        tempRoot.deleteRecursively()
    }

    @Test
    fun `createSessionDir creates the frames directory`() {
        val uuid = "test-uuid-create"
        val framesDir = manager.createSessionDir(uuid)

        assertTrue("frames directory must exist after creation", framesDir.exists())
        assertTrue("frames directory must be a directory", framesDir.isDirectory)
    }

    @Test
    fun `createSessionDir returns path ending in frames`() {
        val uuid = "test-uuid-name"
        val framesDir = manager.createSessionDir(uuid)

        assertEquals("frames", framesDir.name)
    }

    @Test
    fun `createSessionDir creates intermediate directories`() {
        val uuid = "test-uuid-parents"
        val framesDir = manager.createSessionDir(uuid)

        assertTrue("session dir must exist", framesDir.parentFile?.exists() == true)
        assertTrue("sessions root must exist", framesDir.parentFile?.parentFile?.exists() == true)
    }

    @Test
    fun `deleteSessionDir removes the session tree`() {
        val uuid = "test-uuid-delete"
        val framesDir = manager.createSessionDir(uuid)
        val sessionDir = framesDir.parentFile!!

        // Place a file to verify recursive delete
        File(framesDir, "frame_000001.jpg").createNewFile()

        manager.deleteSessionDir(uuid)

        assertFalse("session dir must be gone after delete", sessionDir.exists())
    }

    @Test
    fun `deleteSessionDir on non-existent uuid does not throw`() {
        manager.deleteSessionDir("non-existent-uuid-9999")
    }

    @Test
    fun `createSessionDir called twice for same uuid is idempotent`() {
        val uuid = "test-uuid-idempotent"
        val first = manager.createSessionDir(uuid)
        val second = manager.createSessionDir(uuid)

        assertTrue(first.exists())
        assertTrue(second.exists())
        assertEquals(first.absolutePath, second.absolutePath)
    }
}
