package com.sfm.scanner.data.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.sfm.scanner.core.common.AppDispatchers

/**
 * CameraX-backed frame source implementing the image capture pipeline.
 *
 * Threading model:
 * - [analyze] callback runs on [cameraExecutor] (camera executor thread)
 * - [copyYuvPlanes] and [ImageProxy.close] happen synchronously on camera executor
 * - JPEG encoding runs on [AppDispatchers.io] via a launched child coroutine
 * - [ProcessCameraProvider.bindToLifecycle] runs on Main per CameraX requirement
 */
internal class CameraXFrameSource(
    private val context: Context,
    private val dispatchers: AppDispatchers,
) {

    private val timestampGate = TimestampGate()
    private val inFlightCount = AtomicInteger(0)
    private val frameCounter = AtomicInteger(0)

    @Volatile private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var activeAnalysis: ImageAnalysis? = null
    @Volatile private var activeCamera: Camera? = null
    @Volatile private var cameraExecutor: ExecutorService? = null

    fun asFlow(
        lifecycleOwner: LifecycleOwner,
        config: CameraConfig,
        previewSurfaceProvider: Preview.SurfaceProvider?,
    ): Flow<FrameResult> = callbackFlow {
        timestampGate.reset()
        frameCounter.set(0)
        inFlightCount.set(0)

        val executor = Executors.newSingleThreadExecutor()
        cameraExecutor = executor

        val imageAnalysis = buildImageAnalysis(config)
        activeAnalysis = imageAnalysis

        val preview = previewSurfaceProvider?.let { provider ->
            Preview.Builder().build().also { it.surfaceProvider = provider }
        }

        // Encoder is stateless w.r.t. frames; constructed once per flow so we avoid
        // per-frame allocation (previously `JpegFrameEncoder(quality)` was instantiated
        // inside processFrame for every accepted frame).
        val encoder = JpegFrameEncoder(config.jpegQuality)
        val producerScope = this

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            processFrame(imageProxy, encoder, producerScope)
        }

        val provider = acquireCameraProvider()
        cameraProvider = provider

        val cameraSelector = buildCameraSelector(config.preferredCameraId)

        withContext(dispatchers.main) {
            // Unbind any previous bindings to avoid lingering use cases on rebind
            runCatching { provider.unbindAll() }
            val useCases = listOfNotNull(preview, imageAnalysis).toTypedArray()
            activeCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases,
            )
        }

        awaitClose {
            runCatching { provider.unbindAll() }
            runCatching { executor.shutdown() }
            cameraProvider = null
            activeAnalysis = null
            activeCamera = null
            cameraExecutor = null
        }
    }

    suspend fun stop() {
        withContext(dispatchers.main) {
            runCatching { cameraProvider?.unbindAll() }
        }
        runCatching { activeAnalysis?.clearAnalyzer() }
        activeCamera = null
    }

    suspend fun setTorch(on: Boolean): Boolean {
        val camera = activeCamera ?: return false
        return try {
            withContext(dispatchers.main) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val future = camera.cameraControl.enableTorch(on)
                    future.addListener(
                        {
                            try {
                                future.get()
                                cont.resume(true)
                            } catch (e: Exception) {
                                Log.w(LOG_TAG, "setTorch($on) failed: ${e.message}")
                                cont.resume(false)
                            }
                        },
                        ContextCompat.getMainExecutor(context),
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "setTorch wrapping failure: ${e.message}")
            false
        }
    }

    private fun buildCameraSelector(preferredCameraId: String?): CameraSelector {
        if (preferredCameraId == null) return CameraSelector.DEFAULT_BACK_CAMERA
        return CameraSelector.Builder()
            .addCameraFilter { infos ->
                val match = infos.firstOrNull {
                    Camera2CameraInfo.from(it).cameraId == preferredCameraId
                }
                if (match != null) listOf(match) else infos
            }
            .build()
    }

    private fun processFrame(
        imageProxy: ImageProxy,
        encoder: JpegFrameEncoder,
        scope: ProducerScope<FrameResult>,
    ) {
        val nowElapsed = SystemClock.elapsedRealtime()

        // Layer 1 backpressure: timestamp gate (5 FPS enforcement)
        if (!timestampGate.shouldAccept(nowElapsed)) {
            imageProxy.close()
            return
        }

        // Layer 2 backpressure: cap concurrent encode+emit operations
        if (inFlightCount.get() >= MAX_FRAMES_IN_FLIGHT) {
            Log.d(LOG_TAG, "MAX_FRAMES_IN_FLIGHT reached — dropping frame")
            imageProxy.close()
            return
        }
        inFlightCount.incrementAndGet()

        // Copy YUV data synchronously on camera executor, then release buffer immediately.
        // No ImageProxy reference escapes this block.
        val yuvFrame = copyYuvPlanes(imageProxy)
        val captureTimeMs = System.currentTimeMillis()
        imageProxy.close()

        scope.launch(dispatchers.io) {
            try {
                val jpegBytes = encoder.encode(yuvFrame)
                val index = frameCounter.incrementAndGet()
                val filename = "frame_%06d.jpg".format(index)
                val record = FrameRecord(
                    index = index,
                    filename = filename,
                    absolutePath = filename,
                    timestampMs = captureTimeMs,
                )
                scope.trySend(FrameResult.Frame(record, jpegBytes))
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Frame encode failed: ${e.message}", e)
                scope.trySend(FrameResult.Error(e))
            } finally {
                inFlightCount.decrementAndGet()
            }
        }
    }

    private fun buildImageAnalysis(config: CameraConfig): ImageAnalysis {
        val builder = ImageAnalysis.Builder()
            .setResolutionSelector(ResolutionPicker().buildCameraXSelector())
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

        applyCamera2Interop(builder, config)

        return builder.build()
    }

    private fun applyCamera2Interop(builder: ImageAnalysis.Builder, config: CameraConfig) {
        try {
            val extender = Camera2Interop.Extender(builder)
            // R-02 mitigation: hint the target FPS range to the HAL via Camera2 interop.
            // ImageAnalysis.Builder itself has no setTargetFrameRate(...) in CameraX 1.4;
            // CONTROL_AE_TARGET_FPS_RANGE is the documented channel.
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(config.targetFps, config.targetFps),
            )
            if (config.lockFocus) {
                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF,
                )
            }
            if (config.lockExposure) {
                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_LOCK,
                    true,
                )
            }
        } catch (e: Exception) {
            // R-04 mitigation: Camera2Interop unsupported on this device; proceed with default AF/AE
            Log.w(LOG_TAG, "Camera2Interop setup failed — using default AF/AE: ${e.message}")
        }
    }

    private fun copyYuvPlanes(imageProxy: ImageProxy): YuvFrame {
        val width = imageProxy.width
        val height = imageProxy.height
        val planes = imageProxy.planes

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        val pixelStride = vPlane.pixelStride

        // Y plane: strip row-stride padding to get compact W×H bytes
        val yRowStride = yPlane.rowStride
        val yBytes = ByteArray(width * height)
        val yBuffer = yPlane.buffer
        if (yRowStride == width) {
            yBuffer.get(yBytes)
        } else {
            val startPos = yBuffer.position()
            for (row in 0 until height) {
                yBuffer.position(startPos + row * yRowStride)
                yBuffer.get(yBytes, row * width, width)
            }
        }

        // UV planes: copy all remaining bytes
        val uBytes = uPlane.buffer.toCompactByteArray()
        val vBytes = vPlane.buffer.toCompactByteArray()

        return YuvFrame(yBytes, uBytes, vBytes, pixelStride, width, height)
    }

    private suspend fun acquireCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            cont.invokeOnCancellation { future.cancel(true) }
        }
}

private fun ByteBuffer.toCompactByteArray(): ByteArray {
    val bytes = ByteArray(remaining())
    get(bytes)
    return bytes
}
