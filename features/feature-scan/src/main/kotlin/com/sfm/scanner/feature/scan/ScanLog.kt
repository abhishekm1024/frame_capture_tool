package com.sfm.scanner.feature.scan

import android.util.Log

/**
 * Structured logging helpers for the scan-screen init path.
 *
 * Two output styles:
 *  - [breadcrumb] — `Log.i(tag, "phase=$phase ${kv pairs}")`. One line per init stage so we
 *    can read the timeline from logcat: ARCore session created → camera shared → surface
 *    provider attached → CameraX bind starting → CameraX bind succeeded.
 *  - [logError] — `Log.e(tag, "phase=$phase | class=… | message=… | cause-chain=…", throwable)`.
 *    Walks the entire `cause` chain so wrapped exceptions (RuntimeException(cause=…)) reveal
 *    their root cause inline in the log line, in addition to the stack trace.
 *
 * Why this exists: prior to user-M16, scan-init exceptions were logged as
 * `"Camera startCapture failed: ${e.message}"`. For many CameraX/ARCore exceptions
 * `e.message` is `null` and `e.cause` is the root, so the log line read `... failed: null`
 * with the actual cause buried in the stack trace and discoverable only by reading every
 * line. This helper makes the failing phase and the real cause class greppable.
 */
internal object ScanLog {

    /** Log a single init-phase breadcrumb. */
    fun breadcrumb(tag: String, phase: String, vararg kv: Pair<String, Any?>) {
        Log.i(tag, formatBreadcrumb(phase, kv))
    }

    /**
     * Log an init-path exception with the failing phase, exception class, message, and
     * fully-unwrapped cause chain. The original throwable is also passed to `Log.e` so the
     * stack trace lands in the same log entry.
     */
    fun logError(tag: String, phase: String, throwable: Throwable, vararg kv: Pair<String, Any?>) {
        Log.e(tag, formatError(phase, throwable, kv), throwable)
    }

    internal fun formatBreadcrumb(phase: String, kv: Array<out Pair<String, Any?>>): String =
        buildString {
            append("phase=").append(phase)
            for ((k, v) in kv) {
                append(' ').append(k).append('=').append(v ?: "null")
            }
        }

    internal fun formatError(
        phase: String,
        throwable: Throwable,
        kv: Array<out Pair<String, Any?>>,
    ): String = buildString {
        append("phase=").append(phase)
        for ((k, v) in kv) {
            append(' ').append(k).append('=').append(v ?: "null")
        }
        append(" | class=").append(throwable.javaClass.name)
        append(" | message=").append(throwable.message ?: "<null>")
        append(" | cause-chain=").append(formatCauseChain(throwable))
    }

    internal fun formatCauseChain(throwable: Throwable): String = buildString {
        var current: Throwable? = throwable.cause
        val seen = mutableSetOf(System.identityHashCode(throwable))
        var depth = 0
        while (current != null && depth < CAUSE_CHAIN_LIMIT) {
            val id = System.identityHashCode(current)
            if (id in seen) {
                append("[cycle]")
                return@buildString
            }
            seen += id
            if (isNotEmpty()) append(" -> ")
            append(current.javaClass.simpleName)
            current.message?.takeIf { it.isNotBlank() }?.let { append('(').append(it).append(')') }
            current = current.cause
            depth++
        }
        if (current != null) append(" -> [truncated]")
        if (isEmpty()) append("<none>")
    }

    private const val CAUSE_CHAIN_LIMIT = 8
}
