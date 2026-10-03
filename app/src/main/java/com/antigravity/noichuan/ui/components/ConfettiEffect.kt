package com.antigravity.noichuan.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    val color: Color,
    val initialYRatio: Float
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 45
) {
    val colors = listOf(
        Color(0xFFFFD54F),
        Color(0xFFFF4081),
        Color(0xFF00E676),
        Color(0xFF00B0FF),
        Color(0xFFFF9100),
        Color(0xFFD500F9)
    )

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                speed = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors.random(),
                initialYRatio = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ConfettiTransition")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ConfettiProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val currentY = ((p.initialYRatio + progress.value * p.speed) % 1.0f) * (h + 40f) - 20f
            val currentX = p.xRatio * w + kotlin.math.sin((progress.value * 6.28f + p.initialYRatio * 10f).toDouble()).toFloat() * 20f

            drawRect(
                color = p.color,
                topLeft = Offset(currentX, currentY),
                size = Size(p.size, p.size * 0.7f)
            )
        }
    }
}
