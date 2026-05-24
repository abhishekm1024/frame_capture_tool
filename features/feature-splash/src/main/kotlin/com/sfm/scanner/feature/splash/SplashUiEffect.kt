package com.sfm.scanner.feature.splash

sealed class SplashUiEffect {
    data object NavigateToSelection : SplashUiEffect()
}
