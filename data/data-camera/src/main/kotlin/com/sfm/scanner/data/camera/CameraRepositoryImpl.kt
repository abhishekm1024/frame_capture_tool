package com.sfm.scanner.data.camera

import android.content.Context
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import com.sfm.scanner.core.common.AppDispatchers
import javax.inject.Inject

class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : CameraRepository {

    private val frameSource = CameraXFrameSource(context, dispatchers)

    override fun startCapture(
        lifecycleOwner: LifecycleOwner,
        config: CameraConfig,
        previewSurfaceProvider: Preview.SurfaceProvider?,
    ): Flow<FrameResult> =
        frameSource.asFlow(lifecycleOwner, config, previewSurfaceProvider)

    override suspend fun stopCapture() {
        frameSource.stop()
    }

    override suspend fun setTorch(on: Boolean): Boolean =
        frameSource.setTorch(on)
}
