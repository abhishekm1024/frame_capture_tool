package com.sfm.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.ar.core.ArCoreApk
import com.sfm.scanner.core.ui.theme.ScanAppTheme
import com.sfm.scanner.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ARCore install request state — survives configuration changes via Activity scope.
    private var arInstallRequested: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanAppTheme {
                AppNavHost(
                    onRequestArInstall = ::requestArCoreInstall,
                )
            }
        }
    }

    /**
     * Forwards [com.sfm.scanner.feature.scan.ScanUiEffect.RequestArInstall] to the ARCore APK.
     * Per Google's ARCore guidance, [ArCoreApk.requestInstall] must be called from the Activity;
     * we track [arInstallRequested] so a second call (after user-cancelled or returning) reflects
     * the post-prompt state to ARCore. Errors during install are silently swallowed — the
     * subsequent `ArSessionEvent.Unsupported` from `ArRepository` then surfaces the error UI.
     */
    private fun requestArCoreInstall() {
        runCatching {
            val status = ArCoreApk.getInstance().requestInstall(this, !arInstallRequested)
            if (status == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                arInstallRequested = true
            }
        }
    }
}
