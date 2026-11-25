package com.antigravity.noichuan.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FilterStyle(val title: String, val topEmoji: String, val leftEmoji: String, val rightEmoji: String) {
    BUNNY("Funny Bunny", "🐰", "✨", "🌸"),
    STAR("Star", "🕶️", "⭐", "🔥"),
    CROWN("Crown", "👑", "💎", "✨")
}

@Composable
fun FilterOverlay(
    style: FilterStyle = FilterStyle.BUNNY,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FilterAnimation")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceOffset"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val neonBrush = Brush.linearGradient(
        listOf(
            Color(0xFFFF007F),
            Color(0xFF00F0FF),
            Color(0xFFFFEE00),
            Color(0xFF00FF66)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 3.5.dp,
                brush = neonBrush,
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        // Top head accessory (Bunny ears / Crown / Sunglasses)
        Text(
            text = style.topEmoji,
            fontSize = 58.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (12 + bounceOffset).dp)
        )

        // Left floating sparkle
        Text(
            text = style.leftEmoji,
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 60.dp)
                .scale(sparkleScale)
        )

        // Right floating sparkle
        Text(
            text = style.rightEmoji,
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 60.dp)
                .scale(sparkleScale)
        )
    }
}
