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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.antigravity.noichuan.data.model.Word
import com.antigravity.noichuan.ui.theme.CharacterOrange
import com.antigravity.noichuan.ui.theme.GateDoorway
import com.antigravity.noichuan.ui.theme.GateWood
import com.antigravity.noichuan.ui.theme.RunwayBevel1
import com.antigravity.noichuan.ui.theme.RunwayBevel2
import com.antigravity.noichuan.ui.theme.RunwayLime
import com.antigravity.noichuan.ui.theme.RunwayLimeLight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IsometricFilterCanvas(
    currentWord: Word?,
    nextWord: Word?,
    gateProgress: Float, // 0f (ở xa) -> 1f (đang ở vị trí linh vật) -> 1.5f (lướt qua sau)
    isRunning: Boolean,
    isGateSuccess: Boolean = false,
    isGateFailure: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "runwayMotion")

    // Chuyển động đường băng liên tục khi đang chạy
    val roadOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 600 else 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roadOffset"
    )

    // Chuyển động nhấp nhô của chú linh vật khi chạy
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbingOffset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Vẽ Dải đường chạy Isometric 3D nghiêng từ góc dưới trái lên góc trên phải
        drawIsometricRunway(
            width = width,
            height = height,
            roadOffset = roadOffset
        )

        // 2. Tọa độ của chú linh vật cố định trên đường băng
        val charBaseX = width * 0.35f
        val charBaseY = height * 0.76f + bobbingOffset

        // 3. Vẽ Cổng từ vựng (Gate) di chuyển dọc theo trục đường băng
        // Trục đường chạy từ (width * 0.95f, height * 0.32f) tới (width * 0.15f, height * 0.90f)
        val startGateX = width * 0.90f
        val startGateY = height * 0.36f
        val targetGateX = width * 0.38f
        val targetGateY = height * 0.74f

        val currentGateX = startGateX + (targetGateX - startGateX) * gateProgress.coerceAtMost(1.0f) -
                if (gateProgress > 1.0f) (width * 0.4f * (gateProgress - 1.0f)) else 0f
        val currentGateY = startGateY + (targetGateY - startGateY) * gateProgress.coerceAtMost(1.0f) +
                if (gateProgress > 1.0f) (height * 0.3f * (gateProgress - 1.0f)) else 0f

        val gateScale = 0.55f + 0.45f * gateProgress.coerceAtMost(1.0f)

        // Nếu cổng chưa vượt qua sau lưng nhân vật, vẽ cổng sau hoặc trước
        if (gateProgress <= 1.0f) {
            drawWordGate(
                gateX = currentGateX,
                gateY = currentGateY,
                scale = gateScale,
                wordText = currentWord?.word ?: "",
                isPassed = false,
                isSuccess = isGateSuccess,
                isFailure = isGateFailure
            )
        }

        // 4. Vẽ Chú Linh Vật Hoạt Hình Khối Hộp Cam (Low-poly Fox)
        drawCubeFox(
            charX = charBaseX,
            charY = charBaseY,
            scale = 1.0f,
            isRunning = isRunning
        )

        // Nếu cổng vừa vượt qua con vật (gateProgress > 1.0f)
        if (gateProgress > 1.0f) {
            drawWordGate(
                gateX = currentGateX,
                gateY = currentGateY,
                scale = gateScale,
                wordText = currentWord?.word ?: "",
                isPassed = true,
                isSuccess = isGateSuccess,
                isFailure = isGateFailure
            )
        }

        // 5. Cổng tiếp theo đang ở xa (Next Gate Preview)
        if (gateProgress >= 0.7f && nextWord != null) {
            val nextGateProgress = (gateProgress - 0.7f) / 0.3f * 0.4f
            val nextX = startGateX + (targetGateX - startGateX) * nextGateProgress
            val nextY = startGateY + (targetGateY - startGateY) * nextGateProgress
            drawWordGate(
                gateX = nextX,
                gateY = nextY,
                scale = 0.55f + 0.45f * nextGateProgress,
                wordText = nextWord.word,
                isPassed = false
            )
        }
    }
}

/**
 * Vẽ dải đường chạy nghiêng góc chéo ~40 độ phong cách Isometric chuẩn của app tham khảo
 */
private fun DrawScope.drawIsometricRunway(
    width: Float,
    height: Float,
    roadOffset: Float
) {
    // 4 đỉnh của mặt đường chạy (Lime Green Surface)
    val p1 = Offset(width * -0.2f, height * 0.98f) // Dưới trái mở rộng
    val p2 = Offset(width * 0.58f, height * 0.42f) // Trên trái
    val p3 = Offset(width * 1.25f, height * 0.42f) // Trên phải
    val p4 = Offset(width * 0.45f, height * 0.98f) // Dưới phải

    // Tầng 2 (đáy sâu nhất): Màu xám xanh rêu đậm
    val bottomBevelPath = Path().apply {
        moveTo(p1.x, p1.y + 40f)
        lineTo(p4.x, p4.y + 40f)
        lineTo(p3.x, p3.y + 40f)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p1.x, p1.y)
        close()
    }
    drawPath(path = bottomBevelPath, color = RunwayBevel2)

    // Tầng 1 (cạnh dày trên): Màu xám xanh rêu
    val topBevelPath = Path().apply {
        moveTo(p1.x, p1.y + 20f)
        lineTo(p4.x, p4.y + 20f)
        lineTo(p3.x, p3.y + 20f)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        lineTo(p1.x, p1.y)
        close()
    }
    drawPath(path = topBevelPath, color = RunwayBevel1)

    // Mặt đường chính: Màu xanh chanh neon rực rỡ (Lime Green)
    val runwaySurfacePath = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
    }
    drawPath(path = runwaySurfacePath, color = RunwayLime)

    // Hoa văn các ô vuông điểm xuyết di chuyển (Texture animation)
    for (i in 0..12) {
        val t = ((i * 80f + roadOffset) % 1000f) / 1000f
        val xLeft = p1.x + (p2.x - p1.x) * (1f - t)
        val yLeft = p1.y + (p2.y - p1.y) * (1f - t)
        val xRight = p4.x + (p3.x - p4.x) * (1f - t)
        val yRight = p4.y + (p3.y - p4.y) * (1f - t)

        val squareWidth = 24f + (1f - t) * 20f
        val squareHeight = 14f + (1f - t) * 10f

        val centerX = (xLeft + xRight) / 2f + ((i % 3) - 1) * 45f
        val centerY = (yLeft + yRight) / 2f

        drawRect(
            color = if (i % 2 == 0) RunwayLimeLight.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.35f),
            topLeft = Offset(centerX - squareWidth / 2, centerY - squareHeight / 2),
            size = Size(squareWidth, squareHeight)
        )
    }

    // Viền sọc đỏ - trắng F1 dọc 2 bên mép đường chạy
    val stripeCount = 20
    for (s in 0 until stripeCount) {
        val t1 = s.toFloat() / stripeCount
        val t2 = (s + 0.5f) / stripeCount

        val sColor = if (s % 2 == 0) Color(0xFFD32F2F) else Color.White

        val leftStart = Offset(p1.x + (p2.x - p1.x) * (1f - t1), p1.y + (p2.y - p1.y) * (1f - t1))
        val leftEnd = Offset(p1.x + (p2.x - p1.x) * (1f - t2), p1.y + (p2.y - p1.y) * (1f - t2))
        drawLine(color = sColor, start = leftStart, end = leftEnd, strokeWidth = 8f)

        val rightStart = Offset(p4.x + (p3.x - p4.x) * (1f - t1), p4.y + (p3.y - p4.y) * (1f - t1))
        val rightEnd = Offset(p4.x + (p3.x - p4.x) * (1f - t2), p4.y + (p3.y - p4.y) * (1f - t2))
        drawLine(color = sColor, start = rightStart, end = rightEnd, strokeWidth = 8f)
    }
}

/**
 * Vẽ Chú Linh Vật Hoạt Hình Khối Hộp Cam (Low-poly Fox)
 */
private fun DrawScope.drawCubeFox(
    charX: Float,
    charY: Float,
    scale: Float,
    isRunning: Boolean
) {
    val bodyWidth = 52f * scale
    val bodyHeight = 44f * scale
    val bodyDepth = 36f * scale

    // Bóng đổ mờ của linh vật trên đường chạy
    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(charX - bodyWidth * 0.7f, charY + bodyHeight * 0.7f),
        size = Size(bodyWidth * 1.5f, 20f * scale)
    )

    // Mặt hông trái (Shadow side)
    val leftFace = Path().apply {
        moveTo(charX - bodyWidth / 2, charY)
        lineTo(charX - bodyWidth / 2 - bodyDepth * 0.7f, charY - bodyDepth * 0.5f)
        lineTo(charX - bodyWidth / 2 - bodyDepth * 0.7f, charY - bodyDepth * 0.5f - bodyHeight)
        lineTo(charX - bodyWidth / 2, charY - bodyHeight)
        close()
    }
    drawPath(path = leftFace, color = Color(0xFFE65100))

    // Mặt đỉnh (Top face)
    val topFace = Path().apply {
        moveTo(charX - bodyWidth / 2, charY - bodyHeight)
        lineTo(charX - bodyWidth / 2 - bodyDepth * 0.7f, charY - bodyDepth * 0.5f - bodyHeight)
        lineTo(charX + bodyWidth / 2 - bodyDepth * 0.7f, charY - bodyDepth * 0.5f - bodyHeight)
        lineTo(charX + bodyWidth / 2, charY - bodyHeight)
        close()
    }
    drawPath(path = topFace, color = Color(0xFFFF9800))

    // Mặt trước (Front face)
    val frontFace = Path().apply {
        moveTo(charX - bodyWidth / 2, charY)
        lineTo(charX + bodyWidth / 2, charY)
        lineTo(charX + bodyWidth / 2, charY - bodyHeight)
        lineTo(charX - bodyWidth / 2, charY - bodyHeight)
        close()
    }
    drawPath(path = frontFace, color = CharacterOrange)

    // 2 Tai nhọn hình tam giác 3D
    val earLeft = Path().apply {
        moveTo(charX - bodyWidth * 0.35f, charY - bodyHeight)
        lineTo(charX - bodyWidth * 0.25f, charY - bodyHeight - 20f * scale)
        lineTo(charX - bodyWidth * 0.10f, charY - bodyHeight)
        close()
    }
    drawPath(path = earLeft, color = Color(0xFFBF360C))

    val earRight = Path().apply {
        moveTo(charX + bodyWidth * 0.10f, charY - bodyHeight)
        lineTo(charX + bodyWidth * 0.25f, charY - bodyHeight - 20f * scale)
        lineTo(charX + bodyWidth * 0.35f, charY - bodyHeight)
        close()
    }
    drawPath(path = earRight, color = Color(0xFFBF360C))

    // Mõm trắng và Mũi đen
    drawRect(
        color = Color.White,
        topLeft = Offset(charX + bodyWidth * 0.2f, charY - bodyHeight * 0.5f),
        size = Size(16f * scale, 16f * scale)
    )
    drawCircle(
        color = Color(0xFF1C1B1F),
        radius = 4f * scale,
        center = Offset(charX + bodyWidth * 0.32f, charY - bodyHeight * 0.42f)
    )

    // 2 Mắt đen tròn
    drawCircle(
        color = Color(0xFF1C1B1F),
        radius = 4f * scale,
        center = Offset(charX - bodyWidth * 0.12f, charY - bodyHeight * 0.65f)
    )
    drawCircle(
        color = Color(0xFF1C1B1F),
        radius = 4f * scale,
        center = Offset(charX + bodyWidth * 0.18f, charY - bodyHeight * 0.65f)
    )

    // Điểm sáng trong mắt
    drawCircle(
        color = Color.White,
        radius = 1.5f * scale,
        center = Offset(charX - bodyWidth * 0.14f, charY - bodyHeight * 0.67f)
    )
    drawCircle(
        color = Color.White,
        radius = 1.5f * scale,
        center = Offset(charX + bodyWidth * 0.16f, charY - bodyHeight * 0.67f)
    )
}

/**
 * Vẽ Cổng Chướng Ngại Vật Từ Vựng Bằng Gỗ 3D (Gate with Word text)
 */
private fun DrawScope.drawWordGate(
    gateX: Float,
    gateY: Float,
    scale: Float,
    wordText: String,
    isPassed: Boolean,
    isSuccess: Boolean = false,
    isFailure: Boolean = false
) {
    val gateWidth = 130f * scale
    val gateHeight = 160f * scale
    val doorwayWidth = 64f * scale
    val doorwayHeight = 70f * scale
    val thickness = 30f * scale

    // Màu cổng
    val baseWood = if (isPassed) GateWood.copy(alpha = 0.6f) else GateWood
    val sideWood = if (isPassed) Color(0xFFA17C50).copy(alpha = 0.6f) else Color(0xFFA17C50)
    val topWood = if (isPassed) Color(0xFFDCC19E).copy(alpha = 0.6f) else Color(0xFFDCC19E)

    // Mặt hông dày 3D bên phải
    val rightSide = Path().apply {
        moveTo(gateX + gateWidth / 2, gateY)
        lineTo(gateX + gateWidth / 2 + thickness * 0.7f, gateY - thickness * 0.5f)
        lineTo(gateX + gateWidth / 2 + thickness * 0.7f, gateY - thickness * 0.5f - gateHeight)
        lineTo(gateX + gateWidth / 2, gateY - gateHeight)
        close()
    }
    drawPath(path = rightSide, color = sideWood)

    // Mặt trên 3D
    val topSide = Path().apply {
        moveTo(gateX - gateWidth / 2, gateY - gateHeight)
        lineTo(gateX - gateWidth / 2 + thickness * 0.7f, gateY - thickness * 0.5f - gateHeight)
        lineTo(gateX + gateWidth / 2 + thickness * 0.7f, gateY - thickness * 0.5f - gateHeight)
        lineTo(gateX + gateWidth / 2, gateY - gateHeight)
        close()
    }
    drawPath(path = topSide, color = topWood)

    // Mặt chính diện của cổng gỗ
    val frontGate = Path().apply {
        moveTo(gateX - gateWidth / 2, gateY)
        lineTo(gateX - doorwayWidth / 2, gateY)
        lineTo(gateX - doorwayWidth / 2, gateY - doorwayHeight)
        lineTo(gateX + doorwayWidth / 2, gateY - doorwayHeight)
        lineTo(gateX + doorwayWidth / 2, gateY)
        lineTo(gateX + gateWidth / 2, gateY)
        lineTo(gateX + gateWidth / 2, gateY - gateHeight)
        lineTo(gateX - gateWidth / 2, gateY - gateHeight)
        close()
    }
    drawPath(path = frontGate, color = baseWood)

    // Vòm cửa mở màu nâu đậm (Doorway)
    drawRect(
        color = GateDoorway,
        topLeft = Offset(gateX - doorwayWidth / 2, gateY - doorwayHeight),
        size = Size(doorwayWidth, doorwayHeight)
    )

    // Vẽ chữ Từ Vựng tiếng Anh lên mặt trên cổng (chữ xanh khi đúng, chữ đỏ khi sai)
    if (wordText.isNotBlank()) {
        val textColorArgb = when {
            isSuccess -> android.graphics.Color.parseColor("#00FF66")
            isFailure -> android.graphics.Color.parseColor("#FF1744")
            else -> android.graphics.Color.WHITE
        }
        val paint = android.graphics.Paint().apply {
            color = textColorArgb
            textSize = (22f * scale).coerceAtLeast(14f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }
        drawContext.canvas.nativeCanvas.drawText(
            wordText,
            gateX,
            gateY - gateHeight * 0.65f,
            paint
        )
    }

    // Vẽ Vòng tròn Torus trang trí lơ lửng bên cạnh cổng
    val torusColor = when {
        isSuccess -> Color(0xFF00FF66)
        isFailure -> Color(0xFFFF1744)
        else -> Color(0xFF8E24AA)
    }
    drawCircle(
        color = torusColor,
        radius = 20f * scale,
        center = Offset(gateX - gateWidth * 0.65f, gateY - gateHeight * 0.35f),
        style = Stroke(width = 8f * scale)
    )

    // Hiệu ứng ăn mừng khi phát âm chuẩn (Neon Lime Glow + +10 Điểm)
    if (isSuccess) {
        drawPath(
            path = frontGate,
            color = Color(0xFF00FF66),
            style = Stroke(width = 5f * scale)
        )
        val successPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = (24f * scale).coerceAtLeast(16f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.GREEN)
        }
        drawContext.canvas.nativeCanvas.drawText("✨ +10", gateX, gateY - gateHeight - 10f * scale, successPaint)
    }

    // Hiệu ứng viền đỏ rung khi phát âm sai hoặc mất mạng
    if (isFailure) {
        drawPath(
            path = frontGate,
            color = Color(0xFFFF1744),
            style = Stroke(width = 5f * scale)
        )
        val failurePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF1744")
            textSize = (24f * scale).coerceAtLeast(16f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.RED)
        }
        drawContext.canvas.nativeCanvas.drawText("❌ -1 ❤️", gateX, gateY - gateHeight - 10f * scale, failurePaint)
    }
}
