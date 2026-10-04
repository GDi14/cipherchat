package com.example.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

// ==============================================================================
// Material 3 Expressive Motion Tokens
// Spec: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
// ==============================================================================

object ExpressiveMotion {
    // Easing curves
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    // Duration tokens (ms)
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600

    // Expressive Springs
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Expressive Screen Forward Navigation Transition (Shared Z/X axis with scale)
    val ScreenEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
        animationSpec = tween(DurationLong2, easing = EmphasizedDecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(DurationMedium2, easing = LinearOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.94f,
        animationSpec = tween(DurationLong2, easing = EmphasizedDecelerateEasing)
    )

    val ScreenExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
        animationSpec = tween(DurationMedium4, easing = EmphasizedAccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(DurationShort4, easing = FastOutSlowInEasing)
    ) + scaleOut(
        targetScale = 0.96f,
        animationSpec = tween(DurationMedium4, easing = EmphasizedAccelerateEasing)
    )

    // Expressive Screen Back Navigation Transition
    val ScreenPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.2f).toInt() },
        animationSpec = tween(DurationLong2, easing = EmphasizedDecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(DurationMedium2, easing = LinearOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.96f,
        animationSpec = tween(DurationLong2, easing = EmphasizedDecelerateEasing)
    )

    val ScreenPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.3f).toInt() },
        animationSpec = tween(DurationMedium4, easing = EmphasizedAccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(DurationShort4, easing = FastOutSlowInEasing)
    ) + scaleOut(
        targetScale = 0.94f,
        animationSpec = tween(DurationMedium4, easing = EmphasizedAccelerateEasing)
    )

    // Standard Expressive Forward Transform
    val ForwardTransition: ContentTransform = ScreenEnter togetherWith ScreenExit

    // Standard Expressive Backward Transform
    val BackwardTransition: ContentTransform = ScreenPopEnter togetherWith ScreenPopExit
}
