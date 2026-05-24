package com.sfm.scanner.feature.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun SplashScreen(
    onNavigateToSelection: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    BackHandler { /* no-op: back is disabled on splash */ }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SplashUiEffect.NavigateToSelection -> onNavigateToSelection()
            }
        }
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_logo))
    val lottieAnimatable = rememberLottieAnimatable()

    LaunchedEffect(composition) {
        composition ?: return@LaunchedEffect
        lottieAnimatable.animate(
            composition = composition,
            iterations = 1,
        )
        viewModel.onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { lottieAnimatable.progress },
            modifier = Modifier
                .size(160.dp)
                .semantics { contentDescription = "App logo" },
        )
    }
}
