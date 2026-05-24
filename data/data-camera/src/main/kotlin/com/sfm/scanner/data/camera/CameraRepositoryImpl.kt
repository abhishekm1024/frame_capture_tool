package com.sfm.scanner.data.camera

import android.content.Context
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

    override fun startCapture(lifecycleOwner: LifecycleOwner, config: CameraConfig): Flow<FrameResult> =
        frameSource.asFlow(lifecycleOwner, config)

    override suspend fun stopCapture() {
        frameSource.stop()
    }
}
