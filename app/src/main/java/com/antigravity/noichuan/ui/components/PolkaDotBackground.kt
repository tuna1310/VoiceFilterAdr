package com.antigravity.noichuan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.antigravity.noichuan.ui.theme.BlueBackgroundEnd
import com.antigravity.noichuan.ui.theme.BlueBackgroundStart

@Composable
fun PolkaDotBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val gradientColors = if (isDark) {
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    } else {
        listOf(BlueBackgroundStart, Color(0xFF03A9F4), BlueBackgroundEnd)
    }

    val dotColor = if (isDark) {
        Color(0xFF80DEEA).copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.20f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(gradientColors)
            )

            val spacing = 32f
            val dotRadius = 3f

            val cols = (size.width / spacing).toInt() + 2
            val rows = (size.height / spacing).toInt() + 2

            for (row in 0..rows) {
                val offsetX = if (row % 2 == 1) spacing / 2f else 0f
                for (col in 0..cols) {
                    val cx = col * spacing + offsetX
                    val cy = row * spacing
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
        content()
    }
}
