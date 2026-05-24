package com.sfm.scanner.core.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AppFileProviderTest {

    private lateinit var mockContext: Context
    private lateinit var provider: AppFileProvider
    private lateinit var fakeCache: File
    private lateinit var fakeFiles: File

    private lateinit var tmpRoots: List<File>

    @Before
    fun setUp() {
        fakeCache = Files.createTempDirectory("test_cache").toFile()
        fakeFiles = Files.createTempDirectory("test_files").toFile()
        tmpRoots = listOf(fakeCache, fakeFiles)

        mockContext = mockk()
        every { mockContext.cacheDir } returns fakeCache
        every { mockContext.filesDir } returns fakeFiles

        provider = AppFileProvider(mockContext)
    }

    @After
    fun tearDown() {
        tmpRoots.forEach { it.deleteRecursively() }
    }

    @Test
    fun `sessionsRoot returns cacheDir-sessions`() {
        val expected = File(fakeCache, "sessions")
        assertEquals(expected, provider.sessionsRoot)
    }

    @Test
    fun `uploadsDir returns filesDir-uploads and creates it`() {
        val expected = File(fakeFiles, "uploads")
        val actual = provider.uploadsDir
        assertEquals(expected, actual)
        // mkdirs() is called inside the property getter
        assertEquals(expected.absolutePath, actual.absolutePath)
    }

    @Test
    fun `sessionDir returns correct nested path`() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val expected = File(File(fakeCache, "sessions"), uuid)
        assertEquals(expected, provider.sessionDir(uuid))
    }

    @Test
    fun `framesDir returns correct nested path`() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val expected = File(File(File(fakeCache, "sessions"), uuid), "frames")
        assertEquals(expected, provider.framesDir(uuid))
    }

    @Test
    fun `framesDir is a child of sessionDir`() {
        val uuid = "test-uuid-001"
        val sessionDir = provider.sessionDir(uuid)
        val framesDir = provider.framesDir(uuid)
        assertEquals(sessionDir, framesDir.parentFile)
    }
}
