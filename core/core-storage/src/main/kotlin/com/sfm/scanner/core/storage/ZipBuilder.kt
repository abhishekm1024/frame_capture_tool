package com.sfm.scanner.core.storage

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * DSL wrapper around [ZipOutputStream] that writes atomically via a temp file.
 *
 * Usage:
 * ```
 * zipBuilder.create(destFile) {
 *     addBytes("details.json", jsonBytes)
 *     addFile("frames/frame_000001.jpg", sourceFile)
 * }
 * ```
 *
 * ZIP is written to a `.tmp` sibling file and renamed into place only after
 * [ZipOutputStream.close] succeeds, satisfying the ZIP atomicity contract (R-16).
 *
 * Thread safety: this class is **stateless**. Each [create] call owns its own
 * [ZipOutputStream] inside a [ZipWriteScope]; the receiver lambda holds the stream
 * via the scope rather than via mutable state on the builder. Multiple concurrent
 * [create] calls on the same `ZipBuilder` instance therefore do not interfere.
 */
class ZipBuilder @Inject constructor() {

    /**
     * Per-call write scope. Wraps a single [ZipOutputStream]; lifecycle is bounded by
     * the [create] block. Not retained beyond the lambda — references obtained from
     * inside the block must not escape.
     */
    class ZipWriteScope internal constructor(private val zipStream: ZipOutputStream) {
        fun addFile(entryPath: String, sourceFile: File) {
            zipStream.putNextEntry(ZipEntry(entryPath))
            sourceFile.inputStream().use { it.copyTo(zipStream) }
            zipStream.closeEntry()
        }

        fun addBytes(entryPath: String, bytes: ByteArray) {
            zipStream.putNextEntry(ZipEntry(entryPath))
            zipStream.write(bytes)
            zipStream.closeEntry()
        }
    }

    fun create(destFile: File, block: ZipWriteScope.() -> Unit) {
        val tempFile = File(destFile.parent, "${destFile.name}.tmp")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zip ->
                ZipWriteScope(zip).block()
            }
            if (!tempFile.renameTo(destFile)) {
                throw IOException("Failed to atomically rename ZIP to ${destFile.absolutePath}")
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }
}
