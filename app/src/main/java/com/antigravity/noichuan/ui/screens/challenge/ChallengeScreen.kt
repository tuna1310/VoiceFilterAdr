package com.antigravity.noichuan.ui.screens.challenge

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.antigravity.noichuan.R
import com.antigravity.noichuan.data.service.ScreenRecordManager
import com.antigravity.noichuan.ui.components.CameraPreview
import com.antigravity.noichuan.ui.components.ConfettiEffect
import com.antigravity.noichuan.ui.components.IsometricFilterCanvas
import com.antigravity.noichuan.ui.components.VideoRecorderController
import com.antigravity.noichuan.ui.components.rememberVideoRecorderController
import com.antigravity.noichuan.ui.theme.PrimaryBlue
import java.io.File

@Composable
fun ChallengeScreen(
    viewModel: ChallengeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (
        videoPath: String?,
        subjectKey: String,
        levelKey: String,
        durationSec: Int,
        wordsCompleted: Int,
        score: Int,
        stars: Int,
        combo: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val recorderController = rememberVideoRecorderController()
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    var isUsingScreenRecorder by remember { mutableStateOf(false) }

    val hasCameraPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // Rung phản hồi xúc giác nhẹ khi đúng hoặc sai từ vựng
    LaunchedEffect(uiState.hapticTick) {
        if (uiState.hapticTick > 0L) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Launcher xin quyền MediaProjection để quay toàn bộ màn hình (Camera + AR Overlay 3D + Watermark)
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val metrics = context.resources.displayMetrics
            val file = ScreenRecordManager.generateVideoFile(context)
            currentRecordingFile = file
            isUsingScreenRecorder = true

            val isVosk = viewModel.speechEngine.engineType == com.antigravity.noichuan.domain.service.SpeechEngineType.VOSK
            ScreenRecordManager.start(
                context = context,
                resultCode = result.resultCode,
                resultData = result.data!!,
                width = metrics.widthPixels,
                height = metrics.heightPixels,
                dpi = metrics.densityDpi,
                outputFile = file,
                usePcmAudio = isVosk
            )
            viewModel.toggleRecording()
        } else {
            // Nếu người dùng từ chối cấp quyền quay màn hình, fallback về CameraX VideoCapture
            isUsingScreenRecorder = false
            val file = recorderController.startRecording(
                context = context,
                onStart = { viewModel.toggleRecording() },
                onError = { viewModel.toggleRecording() }
            )
            currentRecordingFile = file
        }
    }

    DisposableEffect(viewModel.speechEngine) {
        if (viewModel.speechEngine.engineType == com.antigravity.noichuan.domain.service.SpeechEngineType.VOSK) {
            val listener: (ShortArray, Int) -> Unit = { buffer, readSize ->
                viewModel.speechEngine.feedPcmData(buffer, readSize)
            }
            ScreenRecordManager.pcmAudioRecorder.getDispatcher().registerListener(listener)
            onDispose {
                ScreenRecordManager.pcmAudioRecorder.getDispatcher().unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pauseGame()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) {
            permLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is RecordingNavEvent.NavigateToPreview -> {
                    onNavigateToPreview(
                        event.videoPath,
                        event.subjectKey,
                        event.levelKey,
                        event.durationSec,
                        event.wordsCompleted,
                        event.totalScore,
                        event.stars,
                        event.maxCombo
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Khi bộ đếm thời gian hết hạn hoặc viewModel tự động dừng quay
    LaunchedEffect(uiState.isRecording) {
        if (!uiState.isRecording && ScreenRecordManager.isRecording) {
            ScreenRecordManager.stop(context, viewModel.gameStateMachine.eventTimeline) { file ->
                currentRecordingFile = null
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Fullscreen Camera Preview (hoặc fallback gradient khi chưa cấp quyền)
            if (hasCameraPerm) {
                CameraPreview(
                    isFrontCamera = uiState.isFrontCamera,
                    isFlashOn = uiState.isFlashOn,
                    controller = recorderController,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                            )
                        )
                )
            }

            // 2. Hiệu ứng Filter AR: Isometric 3D Canvas Runway + Linh vật hoạt hình + Cổng gỗ từ vựng
            IsometricFilterCanvas(
                currentWord = uiState.currentWord,
                nextWord = uiState.nextWord,
                gateProgress = uiState.gateProgress,
                isRunning = uiState.isRecording,
                isGateSuccess = uiState.isGateSuccess,
                isGateFailure = uiState.isWordFailure,
                modifier = Modifier.fillMaxSize()
            )

            // 3. Hiệu ứng Confetti ăn mừng khi phát âm chuẩn
            if (uiState.showConfetti) {
                ConfettiEffect(modifier = Modifier.fillMaxSize())
            }

            // 4. Nút Back góc trên bên trái
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 5. Watermark Thương Hiệu "Nói Chuẩn" + Voice Runner HUD (3 Mạng, Điểm, Combo)
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 18.dp, start = 68.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF00FF66), // Neon lime
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Nói Chuẩn",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "• ${uiState.gameModeDisplayName}",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // 3 Mạng (Hearts) của Voice Runner
                    if (uiState.gameMode == "voice_runner") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (i in 1..uiState.maxLives) {
                                val isAlive = i <= uiState.livesRemaining
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = "Life $i",
                                    tint = if (isAlive) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Điểm số & Combo
                    if (uiState.score > 0) {
                        Text(
                            text = "${uiState.score}đ",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (uiState.combo > 1) {
                        Text(
                            text = "x${uiState.combo}",
                            color = Color(0xFFFF9100),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // 6. Floating Celebration Banner khi nói đúng từ
            AnimatedVisibility(
                visible = uiState.successBannerText != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp)
            ) {
                uiState.successBannerText?.let { bannerText ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.92f),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = bannerText,
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 7. Cột công cụ dọc bên phải (Vertical Toolbar: Flip, Flash, Skin, Lips)
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .align(Alignment.TopEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ToolButton(
                    icon = Icons.Rounded.FlipCameraAndroid,
                    label = "Flip",
                    onClick = { viewModel.toggleCameraFacing() }
                )

                ToolButton(
                    icon = if (uiState.isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                    label = "Flash",
                    isActive = uiState.isFlashOn,
                    onClick = { viewModel.toggleFlash() }
                )

                ToolButton(
                    icon = if (uiState.isSfxEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    label = "SFX",
                    isActive = uiState.isSfxEnabled,
                    onClick = { viewModel.toggleSfx() }
                )

                ToolButton(
                    icon = if (uiState.isBgmEnabled) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                    label = "BGM",
                    isActive = uiState.isBgmEnabled,
                    onClick = { viewModel.toggleBgm() }
                )

                if (uiState.isRecording) {
                    ToolButton(
                        icon = Icons.Rounded.Pause,
                        label = "Pause",
                        onClick = { viewModel.pauseGame() }
                    )
                }
            }

            // 8. Mode specific interactive cards (Tongue Twister & Category Rush)
            if (uiState.gameMode == "tongue_twister" && uiState.currentWord != null) {
                TongueTwisterSentenceCard(
                    sentence = uiState.currentWord?.word ?: "",
                    highlightedIndices = uiState.highlightedWordIndices,
                    phonetic = uiState.currentWord?.phonetic,
                    tip = uiState.currentWord?.tip,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                )
            } else if (uiState.gameMode == "category_rush") {
                CategoryRushCard(
                    subjectTitle = uiState.subject.titleVi,
                    elapsedSeconds = uiState.elapsedSeconds,
                    uniqueWordsCount = uiState.uniqueWordsCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                )
            }

            // 9. Khu vực điều khiển ghi hình ở đáy màn hình
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thanh chọn thời lượng: 15s | 60s | 10m
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DurationOption(
                        text = "15s",
                        isSelected = uiState.selectedDurationSeconds == 15,
                        onClick = { viewModel.selectDuration(15) }
                    )
                    DurationOption(
                        text = "60s",
                        isSelected = uiState.selectedDurationSeconds == 60,
                        onClick = { viewModel.selectDuration(60) }
                    )
                    DurationOption(
                        text = "10m",
                        isSelected = uiState.selectedDurationSeconds == 600,
                        onClick = { viewModel.selectDuration(600) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Nút Quay tròn trắng kèm Circular Progress Ring
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!uiState.isRecording) {
                                // Kiểm tra dung lượng bộ nhớ trống (tối thiểu 100MB)
                                val availableBytes = try {
                                    android.os.StatFs(context.filesDir.absolutePath).availableBytes
                                } catch (e: Exception) {
                                    Long.MAX_VALUE
                                }
                                if (availableBytes < 100L * 1024L * 1024L) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Bộ nhớ thiết bị còn dưới 100MB, vui lòng dọn dẹp dung lượng để tiếp tục quay video.")
                                    }
                                    return@clickable
                                }

                                // Yêu cầu cấp quyền quay toàn bộ màn hình AR
                                val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                                if (projectionManager != null) {
                                    projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                                } else {
                                    // Fallback sang camera thuần nếu không hỗ trợ projection
                                    isUsingScreenRecorder = false
                                    val file = recorderController.startRecording(
                                        context = context,
                                        onStart = { viewModel.toggleRecording() },
                                        onError = { viewModel.toggleRecording() }
                                    )
                                    currentRecordingFile = file
                                }
                            } else {
                                // Dừng quay — path truyền sang ResultScreen
                                if (isUsingScreenRecorder && ScreenRecordManager.isRecording) {
                                    ScreenRecordManager.stop(context, viewModel.gameStateMachine.eventTimeline) { recordedFile ->
                                        val finalPath = recordedFile?.path ?: currentRecordingFile?.path
                                        viewModel.toggleRecording(finalPath)
                                        currentRecordingFile = null
                                    }
                                } else {
                                    recorderController.stopRecording { _ ->
                                        viewModel.toggleRecording(currentRecordingFile?.path)
                                        currentRecordingFile = null
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Vòng tròn tiến độ xanh quét theo chiều kim đồng hồ khi đang quay
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.progressPercent,
                        label = "recordingProgress"
                    )

                    Canvas(modifier = Modifier.size(86.dp)) {
                        if (uiState.isRecording) {
                            // Viền nền mờ
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                style = Stroke(width = 5.dp.toPx())
                            )
                            // Vòng tiến độ màu xanh chính thức
                            drawArc(
                                color = PrimaryBlue,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // Vòng viền ngoài mờ khi chưa quay
                            drawCircle(
                                color = Color.White.copy(alpha = 0.3f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }

                    // Nút bên trong (Tròn trắng khi idle -> Vuông trắng bo góc khi recording)
                    if (uiState.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Watermark thương hiệu góc dưới video (alpha 0.7)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp, end = 16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "Voice Filter: Speak Challenge",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 110.dp)
            )

            // 9. Countdown 3-2-1 Overlay
            if (uiState.countdownRemaining != null && uiState.countdownRemaining!! > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.countdownRemaining}",
                            color = Color(0xFF00FFCC),
                            fontSize = 84.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.game_countdown_ready),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 10. Pause Modal Dialog
            if (uiState.isPaused) {
                AlertDialog(
                    onDismissRequest = { viewModel.resumeGame() },
                    title = {
                        Text(
                            text = stringResource(R.string.game_pause_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(stringResource(R.string.game_score_format, uiState.score))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.game_lives_format, uiState.livesRemaining))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resumeGame() }) {
                            Text(stringResource(R.string.game_pause_resume), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.quitGame() }) {
                            Text(stringResource(R.string.game_pause_quit), color = Color(0xFFFF5252))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isActive) PrimaryBlue else Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.shadow(2.dp)
        )
    }
}

@Composable
private fun DurationOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Chấm tròn nhỏ màu trắng nếu đang chọn
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.shadow(2.dp)
        )
    }
}

@Composable
fun TongueTwisterSentenceCard(
    sentence: String,
    highlightedIndices: Set<Int>,
    phonetic: String?,
    tip: String?,
    modifier: Modifier = Modifier
) {
    val tokens = remember(sentence) {
        sentence.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👅 TONGUE TWISTER",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Danh sách từ trong câu kèm hiệu ứng highlight từng từ khi nói đúng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tokens.forEachIndexed { index, token ->
                    val isHighlighted = highlightedIndices.contains(index)
                    Text(
                        text = token,
                        color = if (isHighlighted) Color(0xFF00FF66) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                }
            }

            if (!phonetic.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = phonetic,
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (!tip.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 $tip",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CategoryRushCard(
    subjectTitle: String,
    elapsedSeconds: Float,
    uniqueWordsCount: Int,
    modifier: Modifier = Modifier
) {
    val remainingSec = (30 - elapsedSeconds.toInt()).coerceAtLeast(0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = BorderStroke(1.5.dp, Color(0xFFFF9100).copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ CATEGORY RUSH",
                    color = Color(0xFFFF9100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Chủ đề: $subjectTitle",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Đếm ngược 30s
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (remainingSec <= 5) Color(0xFFFF2A6D) else Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${remainingSec}s",
                        color = if (remainingSec <= 5) Color(0xFFFF2A6D) else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Số từ đã đúng (không lặp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$uniqueWordsCount từ đúng",
                        color = Color(0xFF00FF66),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
