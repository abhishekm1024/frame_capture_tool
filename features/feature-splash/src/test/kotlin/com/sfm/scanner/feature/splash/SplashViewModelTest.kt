package com.sfm.scanner.feature.splash

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SplashViewModelTest {

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setUp() {
        viewModel = SplashViewModel()
    }

    @Test
    fun `onAnimationEnd emits NavigateToSelection effect`() = runTest {
        viewModel.effects.test {
            viewModel.onAnimationEnd()
            assertEquals(SplashUiEffect.NavigateToSelection, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
