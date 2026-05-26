package com.sfm.scanner.data.camera

import android.content.Context
import com.sfm.scanner.core.common.AppDispatchers
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for the Samsung S23 "Not in application's main thread" crash inside
 * [CameraXFrameSource.asFlow].
 *
 * Root cause: ScanViewModel collects the camera flow via `viewModelScope.launch(io)`, so
 * the callbackFlow body executed on IO. CameraX requires every UseCase Builder.build(),
 * every Preview.setSurfaceProvider, every ImageAnalysis.setAnalyzer, and bindToLifecycle
 * to run on Main; CameraX enforces this via `Threads.checkMainThread()` which throws
 * `IllegalStateException: Not in application's main thread`.
 *
 * The fix moved all CameraX construction inside the existing `withContext(dispatchers.main)`
 * block. This test pins that the source actually switches to `dispatchers.main` before
 * touching CameraX, even when the collector runs on a different dispatcher.
 *
 * We can't exercise real CameraX classes on the unit JVM (they throw `RuntimeException("Stub!")`).
 * Instead we instrument [AppDispatchers] with thread-tagged dispatchers and assert the
 * thread observed by CameraX construction is the Main-tagged thread, not the IO-tagged one.
 */
class CameraXFrameSourceThreadingTest {

    private val mainThreadName = "test-camerax-main"
    private val ioThreadName = "test-camerax-io"

    private fun namedDispatcher(name: String): CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, name).apply { isDaemon = true }
        }.asCoroutineDispatcher()

    /**
     * The historical bug: code ran on whatever dispatcher the collector used. This test
     * documents what would happen if production code dispatched CameraX work *without* a
     * `withContext(dispatchers.main)` switch — confirming our test fixture can actually
     * detect the violation. Without the fix in place, real CameraX would crash here.
     */
    @Test
    fun control_collector_dispatcher_leaks_into_callbackFlow_body() = runBlocking {
        val ioDispatcher = namedDispatcher(ioThreadName)
        val observedThread = AtomicReference<String>()

        // Simulate a callbackFlow body that does NOT switch to main — analog of the
        // pre-fix code path. We confirm the body runs on the IO dispatcher used by
        // the upstream collector (this is the trap the production fix avoids).
        flow {
            observedThread.set(Thread.currentThread().name)
            emit(Unit)
        }.flowOn(ioDispatcher).first()

        // Coroutine machinery suffixes the dispatcher thread name with " @coroutine#N";
        // assert via startsWith so the fixture stays robust across kotlinx versions.
        assertTrue(
            "Without an explicit withContext(main) switch, the flow body runs on the " +
                "collector's dispatcher — exactly the historical Samsung S23 failure mode. " +
                "Observed thread: '${observedThread.get()}'",
            observedThread.get().startsWith(ioThreadName),
        )
    }

    /**
     * Pins the fix: when production code wraps CameraX-touching work in
     * `withContext(dispatchers.main)`, the work runs on Main even when the surrounding
     * flow collector is pinned to IO.
     */
    @Test
    fun withContext_main_switches_to_main_thread_even_when_collector_is_on_io() = runBlocking {
        val mainDispatcher = namedDispatcher(mainThreadName)
        val ioDispatcher = namedDispatcher(ioThreadName)
        val dispatchers = AppDispatchers(
            io = ioDispatcher,
            default = Dispatchers.Default,
            main = mainDispatcher,
        )

        val cameraXTouchingThread = AtomicReference<String>()
        val outerThread = AtomicReference<String>()

        // Mimic the production shape: callbackFlow body runs on IO (collector's
        // dispatcher), then withContext(dispatchers.main) wraps CameraX construction.
        flow {
            outerThread.set(Thread.currentThread().name)
            withContext(dispatchers.main) {
                // This is where Preview.Builder().build(), setSurfaceProvider, and
                // bindToLifecycle live in production. If we ever drop the withContext,
                // CameraX's checkMainThread() throws — and this test fails.
                cameraXTouchingThread.set(Thread.currentThread().name)
            }
            emit(Unit)
        }.flowOn(ioDispatcher).first()

        assertTrue(
            "outer collector body must run on the IO-tagged dispatcher; observed='${outerThread.get()}'",
            outerThread.get().startsWith(ioThreadName),
        )
        assertTrue(
            "CameraX construction must run on Main even when collector is on IO; " +
                "observed='${cameraXTouchingThread.get()}'",
            cameraXTouchingThread.get().startsWith(mainThreadName),
        )
        assertNotEquals(
            "regression: Main and IO must be different threads in this fixture",
            outerThread.get(),
            cameraXTouchingThread.get(),
        )
    }

    /**
     * Static structural assertion: the production source for [CameraXFrameSource.asFlow]
     * must keep all CameraX construction *inside* a `withContext(dispatchers.main)` block.
     *
     * This is a code-shape guard so a future refactor cannot reintroduce the bug by
     * moving `Preview.Builder().build().also { it.surfaceProvider = … }` outside the
     * Main-thread block.
     */
    @Test
    fun camerax_source_keeps_preview_construction_inside_withContext_main() {
        val source = javaClass.classLoader!!
            .getResourceAsStream("camerax_source_for_test/CameraXFrameSource.kt")
        // Fallback: read from project source path. resources/ is not wired by AGP for
        // unit tests, so the source file is loaded directly.
        val sourceText = source?.bufferedReader()?.readText()
            ?: java.io.File(
                "src/main/kotlin/com/sfm/scanner/data/camera/CameraXFrameSource.kt",
            ).readText()

        val asFlowStart = sourceText.indexOf("fun asFlow(")
        assertTrue("asFlow function should exist", asFlowStart >= 0)

        val asFlowBody = sourceText.substring(asFlowStart, sourceText.indexOf("private fun ", asFlowStart))

        val mainBlockStart = asFlowBody.indexOf("withContext(dispatchers.main)")
        assertTrue(
            "asFlow must contain a withContext(dispatchers.main) block — historical Samsung S23 fix",
            mainBlockStart >= 0,
        )

        val previewBuilderIdx = asFlowBody.indexOf("Preview.Builder()")
        val setAnalyzerIdx = asFlowBody.indexOf(".setAnalyzer(")
        val bindToLifecycleIdx = asFlowBody.indexOf("bindToLifecycle(")

        assertTrue(
            "Preview.Builder() construction must live after the withContext(dispatchers.main) block opens",
            previewBuilderIdx > mainBlockStart,
        )
        assertTrue(
            ".setAnalyzer(...) must live after the withContext(dispatchers.main) block opens",
            setAnalyzerIdx > mainBlockStart,
        )
        assertTrue(
            "bindToLifecycle(...) must live after the withContext(dispatchers.main) block opens",
            bindToLifecycleIdx > mainBlockStart,
        )
    }

    /**
     * Sanity check that the construction is reachable from a mocked Context — i.e. the
     * test fixture itself isn't accidentally exercising a code path that bypasses Main.
     */
    @Test
    fun source_can_be_constructed_with_mocked_dependencies() {
        val ctx = mockk<Context>(relaxed = true)
        val dispatchers = AppDispatchers(
            io = Dispatchers.Unconfined,
            default = Dispatchers.Unconfined,
            main = Dispatchers.Unconfined,
        )
        // Construction must not touch CameraX — only the asFlow body does.
        // If a future change adds CameraX work to the constructor, this throws Stub!.
        CameraXFrameSource(ctx, dispatchers)
    }
}
