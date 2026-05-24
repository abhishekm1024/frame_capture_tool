package com.sfm.scanner.core.storage

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipFile

class ZipBuilderTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zip_test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `create produces valid ZIP with addBytes entry`() {
        val destFile = File(tempDir, "output.zip")
        val content = "hello world"

        ZipBuilder().create(destFile) {
            addBytes("test.txt", content.toByteArray(Charsets.UTF_8))
        }

        assertTrue(destFile.exists())
        ZipFile(destFile).use { zip ->
            val entry = zip.getEntry("test.txt")
            assertNotNull(entry)
            val actual = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            assertEquals(content, actual)
        }
    }

    @Test
    fun `create produces valid ZIP with addFile entry`() {
        val sourceFile = File(tempDir, "frame_000001.jpg")
        sourceFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))

        val destFile = File(tempDir, "output.zip")

        ZipBuilder().create(destFile) {
            addFile("frames/frame_000001.jpg", sourceFile)
        }

        ZipFile(destFile).use { zip ->
            val entry = zip.getEntry("frames/frame_000001.jpg")
            assertNotNull(entry)
            val bytes = zip.getInputStream(entry).readBytes()
            assertEquals(3, bytes.size)
        }
    }

    @Test
    fun `create uses forward slash as path separator for nested entries`() {
        val destFile = File(tempDir, "output.zip")

        ZipBuilder().create(destFile) {
            addBytes("frames/frame_000001.jpg", byteArrayOf(1))
            addBytes("details.json", "{}".toByteArray())
            addBytes("measurements.json", "{}".toByteArray())
        }

        ZipFile(destFile).use { zip ->
            assertNotNull("forward-slash path must be the entry name",
                zip.getEntry("frames/frame_000001.jpg"))
            assertNotNull(zip.getEntry("details.json"))
            assertNotNull(zip.getEntry("measurements.json"))
        }
    }

    @Test
    fun `create adds multiple entries in insertion order`() {
        val destFile = File(tempDir, "output.zip")
        val entries = listOf("a.txt", "b.txt", "c.txt")

        ZipBuilder().create(destFile) {
            entries.forEach { name -> addBytes(name, name.toByteArray()) }
        }

        ZipFile(destFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(entries, names)
        }
    }

    @Test
    fun `create writes content correctly via addBytes for JSON`() {
        val destFile = File(tempDir, "output.zip")
        val json = """{"key":"value"}"""

        ZipBuilder().create(destFile) {
            addBytes("details.json", json.toByteArray(Charsets.UTF_8))
        }

        ZipFile(destFile).use { zip ->
            val entry = zip.getEntry("details.json")
            val actual = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            assertEquals(json, actual)
        }
    }

    @Test
    fun `create cleans up temp file when exception occurs`() {
        val destFile = File(tempDir, "output.zip")
        val tempFile = File(tempDir, "output.zip.tmp")

        try {
            ZipBuilder().create(destFile) {
                throw IOException("Simulated write failure")
            }
        } catch (_: IOException) {
        }

        assertTrue("dest file must not exist after failure", !destFile.exists())
        assertTrue("temp file must be cleaned up after failure", !tempFile.exists())
    }

    @Test
    fun `create with addFile preserves byte-exact content`() {
        val originalBytes = ByteArray(256) { it.toByte() }
        val sourceFile = File(tempDir, "binary.bin")
        sourceFile.writeBytes(originalBytes)

        val destFile = File(tempDir, "output.zip")

        ZipBuilder().create(destFile) {
            addFile("binary.bin", sourceFile)
        }

        ZipFile(destFile).use { zip ->
            val entry = zip.getEntry("binary.bin")
            val readBack = zip.getInputStream(entry).readBytes()
            assertTrue(originalBytes.contentEquals(readBack))
        }
    }
}
