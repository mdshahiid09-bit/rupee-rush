package com.rupeerush.user.core.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ConfettiParticle(
    val xRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun ConfettiView(
    isActive: Boolean,
    onComplete: () -> Unit = {}
) {
    if (!isActive) return

    val particles = remember {
        val colors = listOf(
            Color(0xFF00C853),
            Color(0xFFFFD600),
            Color(0xFF2979FF),
            Color(0xFFFF1744),
            Color(0xFFAA00FF)
        )
        List(60) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                speed = Random.nextFloat() * 0.5f + 0.5f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors.random()
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1600, easing = LinearEasing)
        )
        onComplete()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val t = progress.value

        particles.forEach { p ->
            val currentY = canvasHeight * t * p.speed
            val currentX = (p.xRatio * canvasWidth) + kotlin.math.sin(t * 10f + p.xRatio * 100f) * 20f

            drawRect(
                color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                topLeft = Offset(currentX, currentY),
                size = Size(p.size, p.size)
            )
        }
    }
}
