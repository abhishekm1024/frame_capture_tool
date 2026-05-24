package com.sfm.scanner.data.ar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull

/**
 * Instrumented tests for [DepthAvailabilityChecker].
 *
 * These tests require a device or emulator with ARCore installed.
 * Tests are conditionally skipped via [assumeTrue] if ARCore is not supported on the test device.
 */
@RunWith(AndroidJUnit4::class)
class DepthAvailabilityCheckerTest {

    private lateinit var context: Context
    private val checker = DepthAvailabilityChecker()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        assumeTrue(
            "ARCore not supported on this device — skipping AR tests",
            availability == ArCoreApk.Availability.SUPPORTED_INSTALLED,
        )
    }

    @Test
    fun buildConfig_returns_non_null_config() {
        val session = com.google.ar.core.Session(context)
        try {
            val config = checker.buildConfig(session)
            assertNotNull(config)
        } finally {
            session.close()
        }
    }

    @Test
    fun buildConfig_sets_AUTOMATIC_depth_when_supported_or_DISABLED_when_not() {
        val session = com.google.ar.core.Session(context)
        try {
            val isSupported = checker.isDepthSupported(session)
            val config = checker.buildConfig(session)
            val expectedDepthMode = if (isSupported) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }
            org.junit.Assert.assertEquals(expectedDepthMode, config.depthMode)
        } finally {
            session.close()
        }
    }

    @Test
    fun buildConfig_always_enables_instant_placement() {
        val session = com.google.ar.core.Session(context)
        try {
            val config = checker.buildConfig(session)
            org.junit.Assert.assertEquals(
                Config.InstantPlacementMode.LOCAL_Y_UP,
                config.instantPlacementMode,
            )
        } finally {
            session.close()
        }
    }
}
