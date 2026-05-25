package com.sfm.scanner.core.storage

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * Pins the stateless contract introduced for C-21: a single shared [ZipBuilder]
 * instance must support concurrent [ZipBuilder.create] calls without entries
 * crossing between archives.
 *
 * Before the C-21 fix, `ZipBuilder` held the active `ZipOutputStream` in a
 * `private lateinit var` field on the singleton, so the second concurrent call
 * would overwrite the field and the two writes would interleave into one stream.
 * The post-fix design moves the stream into a per-call `ZipWriteScope` that the
 * lambda receives implicitly.
 */
class ZipBuilderConcurrencyTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("zip_concurrency").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `single ZipBuilder instance produces correct archives for concurrent calls`() {
        val builder = ZipBuilder()
        val pool = Executors.newFixedThreadPool(THREAD_COUNT)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(THREAD_COUNT)
        val zipFiles = (1..THREAD_COUNT).map { idx -> File(tempDir, "out-$idx.zip") }
        val errors = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()

        for (idx in 1..THREAD_COUNT) {
            pool.submit {
                try {
                    startLatch.await()
                    builder.create(zipFiles[idx - 1]) {
                        repeat(ENTRIES_PER_ZIP) { entryIdx ->
                            val name = "entry-$idx-$entryIdx.txt"
                            val content = "from-thread-$idx-entry-$entryIdx"
                            addBytes(name, content.toByteArray(Charsets.UTF_8))
                        }
                    }
                } catch (t: Throwable) {
                    errors += t
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertTrue("all threads finish in time", doneLatch.await(15, TimeUnit.SECONDS))
        pool.shutdown()

        assertTrue("no thread errored: $errors", errors.isEmpty())

        for (idx in 1..THREAD_COUNT) {
            val zip = zipFiles[idx - 1]
            assertTrue("zip $idx exists", zip.exists())
            ZipFile(zip).use { archive ->
                val entryNames = archive.entries().asSequence().map { it.name }.toSet()
                assertEquals(
                    "zip $idx contains exactly its own entries (no cross-talk)",
                    (0 until ENTRIES_PER_ZIP).map { "entry-$idx-$it.txt" }.toSet(),
                    entryNames,
                )
                // Spot-check content for one entry per zip.
                val entry = archive.getEntry("entry-$idx-0.txt")
                assertNotNull(entry)
                val text = archive.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
                assertEquals("from-thread-$idx-entry-0", text)
            }
        }
    }

    companion object {
        private const val THREAD_COUNT = 4
        private const val ENTRIES_PER_ZIP = 20
    }
}
