package com.antigravity.noichuan.ui.screens.result

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.antigravity.noichuan.data.local.RecordedVideoEntity
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.domain.repository.VideoRepository
import com.antigravity.noichuan.ui.theme.BackgroundLight
import com.antigravity.noichuan.ui.theme.PrimaryBlue
import com.antigravity.noichuan.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File

import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import com.antigravity.noichuan.ui.components.ConfettiEffect

@OptIn(UnstableApi::class)
@Composable
fun ResultScreen(
    videoPath: String?,
    subjectKey: String,
    levelKey: String,
    durationSec: Int,
    wordsCompleted: Int,
    score: Int = 0,
    stars: Int = 1,
    combo: Int = 0,
    onNavigateBackToHome: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
    videoRepository: VideoRepository = koinInject()
) {
    val subject = Subject.fromKey(subjectKey)
    val level = DifficultyLevel.fromKey(levelKey)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSaved by remember { mutableStateOf(false) }

    val hasRealVideo = remember(videoPath) {
        !videoPath.isNullOrBlank() && File(videoPath).exists() && File(videoPath).length() > 0
    }

    // Khởi tạo ExoPlayer để xem lại video vừa quay
    val exoPlayer = remember(videoPath, hasRealVideo) {
        if (hasRealVideo && videoPath != null) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (stars >= 2) {
                ConfettiEffect(modifier = Modifier.fillMaxSize(), particleCount = 50)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F1FD))
                            .clickable { onNavigateBackToHome() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Home",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Challenge Complete!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Star Rating Row (1-3 stars)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        if (i <= stars) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Star $i",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.StarOutline,
                                contentDescription = "Empty Star $i",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Summary Cards (Score, Words, Max Combo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Điểm số
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Score", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("$score", fontSize = 18.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Từ đúng
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Words", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("$wordsCompleted", fontSize = 18.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Combo cao nhất
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Max Combo", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("${combo}x", fontSize = 18.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Video Preview Card (Post-recording video container)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                    ) {
                        if (hasRealVideo && exoPlayer != null) {
                            // Phát video thật bằng ExoPlayer
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Video Mock Center Preview nếu chưa có file
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Challenge Video (${durationSec}s)",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Practiced $wordsCompleted words",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Badge Level & Subject ở góc trên bên trái
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(level.color)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${subject.titleVi} • ${level.label}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Watermark Nói Chuẩn góc trên bên phải
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Speak Right",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row: Retake | Save | Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nút Retake
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Play Again",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Again",
                            color = PrimaryBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Nút Save to My Videos & Gallery
                    Button(
                        onClick = {
                            scope.launch {
                                val targetFile = if (hasRealVideo && videoPath != null) {
                                    File(videoPath)
                                } else {
                                    File(context.filesDir, "videos").mkdirs()
                                    File(context.filesDir, "videos/recording_${System.currentTimeMillis()}.mp4").apply {
                                        if (!exists()) writeBytes(ByteArray(1024))
                                    }
                                }

                                // 1. Lưu vào Room DB
                                val entity = RecordedVideoEntity(
                                    subjectKey = subject.key,
                                    subjectTitle = subject.titleVi,
                                    difficulty = level.label,
                                    durationSeconds = durationSec,
                                    filePath = targetFile.absolutePath,
                                    wordsCompleted = wordsCompleted
                                )
                                videoRepository.saveVideo(entity)

                                // 2. Xuất bản copy ra thư viện máy (Movies/NoiChuan)
                                withContext(Dispatchers.IO) {
                                    exportVideoToGallery(context, targetFile)
                                }

                                isSaved = true
                                snackbarHostState.showSnackbar("Video saved to Gallery & My Videos!")
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) Color(0xFF4CAF50) else PrimaryBlue
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.BookmarkAdded else Icons.Rounded.Download,
                            contentDescription = "Save Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSaved) "Saved" else "Save Video",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Nút Share
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F1FD))
                            .clickable {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        if (hasRealVideo && videoPath != null) {
                                            val file = File(videoPath)
                                            val contentUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        } else {
                                            type = "text/plain"
                                        }
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "I just completed the ${subject.titleEn} pronunciation challenge on Voice Filter: Speak Challenge!"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Challenge Video"))
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Could not share: ${e.message}")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            )
        }
    }
}

/**
 * Xuất video ra thư viện máy công khai (Movies/NoiChuan) qua MediaStore để xuất hiện trong Google Photos/Gallery
 */
private fun exportVideoToGallery(context: Context, sourceFile: File): Uri? {
    return try {
        if (!sourceFile.exists()) return null

        val filename = "NoiChuan_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/NoiChuan")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
