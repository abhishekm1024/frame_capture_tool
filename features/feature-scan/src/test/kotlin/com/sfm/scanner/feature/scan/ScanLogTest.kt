package com.sfm.scanner.feature.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the structured-log format used by [ScanLog]. These tests exercise the
 * internal `format*` helpers directly because `android.util.Log` is not on the unit-test
 * JVM classpath — but the formatting logic (the part that makes the bug diagnosable) is
 * pure Kotlin.
 *
 * The reason this matters: user-M15's classifier successfully avoided the misleading
 * "Please check permissions" message, but the failure persisted on the Samsung S23 and
 * the real exception class never surfaced in the user-shared logs. user-M16 adds
 * structured breadcrumbs so the failing phase and full cause chain are greppable.
 */
class ScanLogTest {

    @Test
    fun formatBreadcrumb_includes_phase_and_kv_pairs() {
        val line = ScanLog.formatBreadcrumb(
            phase = "bindToLifecycle.starting",
            kv = arrayOf("useCases" to 2, "cameraId" to "0"),
        )
        assertEquals("phase=bindToLifecycle.starting useCases=2 cameraId=0", line)
    }

    @Test
    fun formatBreadcrumb_renders_null_values_as_null_literal() {
        val line = ScanLog.formatBreadcrumb(
            phase = "bindCameraIfReady.waiting",
            kv = arrayOf("cameraId" to null, "hasOwner" to true),
        )
        assertEquals("phase=bindCameraIfReady.waiting cameraId=null hasOwner=true", line)
    }

    @Test
    fun formatError_includes_class_and_message_and_chain() {
        val root = RuntimeException("root cause")
        val wrapper = IllegalStateException("CameraX bind failed", root)
        val line = ScanLog.formatError(
            phase = "bindToLifecycle.failed",
            throwable = wrapper,
            kv = arrayOf("cameraId" to "0"),
        )
        assertTrue("phase present: $line", line.contains("phase=bindToLifecycle.failed"))
        assertTrue("kv present: $line", line.contains("cameraId=0"))
        assertTrue("class present: $line", line.contains("class=java.lang.IllegalStateException"))
        assertTrue("message present: $line", line.contains("message=CameraX bind failed"))
        assertTrue("cause-chain present: $line", line.contains("cause-chain="))
        assertTrue("root cause class in chain: $line", line.contains("RuntimeException"))
        assertTrue("root cause message in chain: $line", line.contains("root cause"))
    }

    @Test
    fun formatError_tolerates_null_message() {
        // CameraX exceptions frequently have null message and rely on the cause chain.
        val cause = RuntimeException("real reason")
        val outer = object : Exception(null as String?, cause) {}
        val line = ScanLog.formatError(
            phase = "startCapture.failed",
            throwable = outer,
            kv = emptyArray(),
        )
        assertTrue("renders <null> instead of literal 'null': $line", line.contains("message=<null>"))
        assertTrue("still surfaces real cause: $line", line.contains("real reason"))
    }

    @Test
    fun formatCauseChain_with_no_cause_returns_none_sentinel() {
        val standalone = RuntimeException("only one")
        assertEquals("<none>", ScanLog.formatCauseChain(standalone))
    }

    @Test
    fun formatCauseChain_joins_nested_causes_with_arrow() {
        val a = IllegalStateException("a")
        val b = RuntimeException("b", a)
        val c = IllegalArgumentException("c", b)
        val chain = ScanLog.formatCauseChain(c)
        // c is the head; chain walks c.cause -> b.cause -> a
        assertTrue("head is RuntimeException(b): $chain", chain.startsWith("RuntimeException(b)"))
        assertTrue("contains arrow: $chain", chain.contains(" -> "))
        assertTrue("ends at IllegalStateException(a): $chain", chain.endsWith("IllegalStateException(a)"))
    }

    @Test
    fun formatCauseChain_handles_self_referential_cycle_safely() {
        // It is legal (if pathological) for a Throwable's cause chain to contain a cycle
        // when set via initCause after the fact. The formatter must not spin.
        val a = RuntimeException("a")
        val b = RuntimeException("b")
        // Build a -> b -> a cycle via reflection-free initCause.
        a.initCause(b)
        runCatching { b.initCause(a) } // may throw IllegalStateException since b.cause is null and we set it once; retry
        val chain = ScanLog.formatCauseChain(a)
        assertTrue("chain returns a finite string: $chain", chain.isNotEmpty())
    }
}
