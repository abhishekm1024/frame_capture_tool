package com.sfm.scanner.feature.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _effects = MutableSharedFlow<SplashUiEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<SplashUiEffect> = _effects.asSharedFlow()

    fun onAnimationEnd() {
        _effects.tryEmit(SplashUiEffect.NavigateToSelection)
    }
}
