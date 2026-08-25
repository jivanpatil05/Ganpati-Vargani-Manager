package com.ganpati.vargani.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganpati.vargani.R
import com.ganpati.vargani.core.theme.BrandGradientMid
import com.ganpati.vargani.core.theme.GoldAccent
import com.ganpati.vargani.core.theme.LightBackground
import com.ganpati.vargani.core.theme.OrangePrimary
import com.ganpati.vargani.core.theme.OrangePrimaryDark
import com.ganpati.vargani.core.theme.VarganiTheme
import com.ganpati.vargani.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SPLASH_DURATION_MS = 1800L

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun resolveDestination(onResult: (loggedIn: Boolean) -> Unit) {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)
            onResult(authRepository.isLoggedIn())
        }
    }
}

@Composable
fun SplashRoute(
    onLoggedIn: () -> Unit,
    onNeedAuth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.resolveDestination { loggedIn ->
            if (loggedIn) onLoggedIn() else onNeedAuth()
        }
    }
    SplashScreen(modifier = modifier)
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    val fade = remember { Animatable(0f) }
    val scale = remember { Animatable(0.72f) }

    LaunchedEffect(Unit) {
        fade.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        )
    }

    val logoDescription = stringResource(R.string.cd_ganpati_logo)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OrangePrimaryDark,
                        OrangePrimary,
                        BrandGradientMid,
                        GoldAccent.copy(alpha = 0.85f),
                        LightBackground,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .alpha(fade.value)
                .scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "ॐ",
                modifier = Modifier.semantics { contentDescription = logoDescription },
                fontSize = 120.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    VarganiTheme {
        SplashScreen()
    }
}
