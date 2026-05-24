package com.sfm.scanner.data.camera

/**
 * Metadata for a single accepted camera frame.
 *
 * [absolutePath] is set to the filename only at the camera layer (e.g. "frame_000001.jpg").
 * The consumer (ScanViewModel) replaces it with the full path after writing the JPEG to disk.
 */
data class FrameRecord(
    val index: Int,
    val filename: String,
    val absolutePath: String,
    val timestampMs: Long,
)
