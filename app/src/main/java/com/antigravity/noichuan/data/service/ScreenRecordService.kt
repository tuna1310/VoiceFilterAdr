package com.antigravity.noichuan.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.antigravity.noichuan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service thực hiện quay toàn bộ màn hình (Camera + AR Overlay 3D + Watermark)
 * sử dụng MediaProjection + VirtualDisplay + MediaRecorder.
 * 
 * KIẾN TRÚC ÂM THANH (Phương án b):
 * Service này CHỈ quay video thuần (Surface), HOÀN TOÀN KHÔNG chạm vào AudioSource.MIC.
 * Luồng âm thanh được thu độc lập bởi PcmAudioRecorder (Single AudioRecord) và ghép hậu kỳ
 * bằng PostRecordAvMuxer để tránh xung đột microphone.
 */
class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val width = intent.getIntExtra(EXTRA_WIDTH, 720)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1280)
                val dpi = intent.getIntExtra(EXTRA_DPI, 320)
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)

                if (resultData != null && filePath != null) {
                    startForegroundWithNotification()
                    startRecording(resultCode, resultData, width, height, dpi, File(filePath))
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Speak Right — Pronunciation Challenge")
            .setContentText("Recording video with AR effects...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording(
        resultCode: Int,
        resultData: Intent,
        width: Int,
        height: Int,
        dpi: Int,
        file: File
    ) {
        outputFile = file
        file.parentFile?.mkdirs()

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            // Setup MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            val recorder = mediaRecorder ?: return

            // Chuẩn hóa độ phân giải chẵn chia hết cho 16
            val recordWidth = (width / 16) * 16
            val recordHeight = (height / 16) * 16

            // MediaRecorder CHỈ thu Video từ Surface, KHÔNG gọi setAudioSource(MIC)
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(file.absolutePath)
            recorder.setVideoSize(recordWidth, recordHeight)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoEncodingBitRate(6 * 1000 * 1000) // 6 Mbps
            recorder.setVideoFrameRate(30)

            recorder.prepare()

            val surface = recorder.surface
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "NoiChuanARVirtualDisplay",
                recordWidth,
                recordHeight,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )

            recorder.start()
            ScreenRecordManager.isRecording = true
            ScreenRecordManager.currentOutputFile = file
            Log.d(TAG, "Screen video-only recording started to: ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording", e)
            ScreenRecordManager.isRecording = false
            ScreenRecordManager.lastError = e.message
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder", e)
        } finally {
            mediaRecorder = null
        }

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display", e)
        } finally {
            virtualDisplay = null
        }

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media projection", e)
        } finally {
            mediaProjection = null
        }

        ScreenRecordManager.isRecording = false
        val saved = outputFile
        Log.d(TAG, "Screen video-only stopped, raw file: ${saved?.absolutePath}")
        ScreenRecordManager.onServiceStopped(saved)
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification while recording AR challenge video"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "ScreenRecordService"
        const val CHANNEL_ID = "noichuan_screen_record_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_START = "com.antigravity.noichuan.action.START_SCREEN_RECORD"
        const val ACTION_STOP = "com.antigravity.noichuan.action.STOP_SCREEN_RECORD"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_DPI = "extra_dpi"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
}

/**
 * Trình quản lý điều khiển ScreenRecordService từ Jetpack Compose UI
 * Tích hợp Single AudioRecord (PcmAudioRecorder) và Bộ ghép Audio-Video hậu kỳ (PostRecordAvMuxer).
 */
object ScreenRecordManager {
    private const val TAG = "ScreenRecordManager"

    var isRecording: Boolean = false
    var currentOutputFile: File? = null
    var lastError: String? = null
    var onRecordFinished: ((File?) -> Unit)? = null
    var currentTimeline: com.antigravity.noichuan.domain.game.GameEventTimeline? = null
    private var appContext: Context? = null

    // PcmAudioRecorder đơn & PostRecordAvMuxer
    val pcmAudioRecorder = PcmAudioRecorder()
    private val avMuxer = PostRecordAvMuxer()

    private var rawVideoFile: File? = null
    private var pcmAudioFile: File? = null
    private var finalTargetFile: File? = null

    private var videoStartNanos: Long = 0L
    private var audioStartNanos: Long = 0L

    /**
     * Cờ đánh dấu phương án fallback nếu tiến trình Mux hậu kỳ gặp sự cố.
     * True nếu phải trả về video không tiếng để bảo vệ file của người dùng.
     */
    var isMuxFallbackUsed: Boolean = false

    /**
     * Chế độ âm thanh:
     * True khi dùng Vosk (Single AudioRecord thu âm PCM và mux hậu kỳ).
     * False khi dùng Google SpeechRecognizer (Google chiếm mic, MediaRecorder không mở AudioRecord).
     */
    var usePcmAudio: Boolean = true

    fun generateVideoFile(context: Context): File {
        val dir = File(context.filesDir, "videos").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "noichuan_ar_$name.mp4")
    }

    fun start(
        context: Context,
        resultCode: Int,
        resultData: Intent,
        width: Int,
        height: Int,
        dpi: Int,
        outputFile: File,
        usePcmAudio: Boolean = this.usePcmAudio
    ) {
        this.usePcmAudio = usePcmAudio
        finalTargetFile = outputFile
        isMuxFallbackUsed = false
        appContext = context.applicationContext

        val dir = File(context.filesDir, "videos").also { it.mkdirs() }
        val timestamp = System.currentTimeMillis()
        val rawVideo = File(dir, "temp_raw_video_$timestamp.mp4")
        rawVideoFile = rawVideo

        if (usePcmAudio) {
            val rawAudio = File(dir, "temp_raw_audio_$timestamp.pcm")
            pcmAudioFile = rawAudio

            // 1. Khởi động PcmAudioRecorder (Single AudioRecord)
            pcmAudioRecorder.startRecording(
                outputFile = rawAudio,
                onStarted = {
                    audioStartNanos = pcmAudioRecorder.audioStartNanos
                    Log.d(TAG, "PcmAudioRecorder started successfully at nano: $audioStartNanos")
                },
                onError = { err ->
                    Log.w(TAG, "PcmAudioRecorder warning: $err")
                }
            )
        } else {
            Log.d(TAG, "usePcmAudio=false (Google mode). Skipping PcmAudioRecorder to avoid mic conflict.")
            pcmAudioFile = null
        }

        // 2. Khởi động ScreenRecordService quay video thuần Surface
        videoStartNanos = SystemClock.elapsedRealtimeNanos()
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, resultData)
            putExtra(ScreenRecordService.EXTRA_WIDTH, width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, height)
            putExtra(ScreenRecordService.EXTRA_DPI, dpi)
            putExtra(ScreenRecordService.EXTRA_FILE_PATH, rawVideo.absolutePath)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(
        context: Context,
        timeline: com.antigravity.noichuan.domain.game.GameEventTimeline? = null,
        onComplete: (File?) -> Unit
    ) {
        onRecordFinished = onComplete
        currentTimeline = timeline

        // 1. Dừng PcmAudioRecorder
        try {
            pcmAudioRecorder.stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping pcmAudioRecorder: ${e.message}")
        }

        // 2. Dừng ScreenRecordService
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun stop(context: Context, onComplete: (File?) -> Unit) {
        stop(context, null, onComplete)
    }

    internal fun onServiceStopped(savedRawVideo: File?) {
        val target = finalTargetFile
        val pcm = pcmAudioFile
        val rawVideo = savedRawVideo ?: rawVideoFile

        if (target != null && rawVideo != null && rawVideo.exists() && pcm != null && pcm.exists() && pcm.length() > 0) {
            // Chạy mux ghép Audio PCM vào MP4 trong IO Coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Executing PostRecordAvMuxer: rawVideo=${rawVideo.length()}B, pcm=${pcm.length()}B")
                    val muxResult = avMuxer.mux(
                        videoOnlyFile = rawVideo,
                        pcmAudioFile = pcm,
                        videoStartNanos = videoStartNanos,
                        audioStartNanos = audioStartNanos,
                        outputMuxedFile = target,
                        timeline = currentTimeline,
                        context = appContext
                    )

                    val resultFile = if (muxResult.isAudioMuxed && target.exists() && target.length() > 0) {
                        Log.d(TAG, "A/V Muxing completed successfully: ${target.absolutePath}")
                        isMuxFallbackUsed = false
                        // Xóa các file tạm sau khi mux thành công
                        pcm.delete()
                        rawVideo.delete()
                        target
                    } else {
                        Log.w(TAG, "A/V Muxing failed (${muxResult.errorMessage}). Fallback to raw video.")
                        isMuxFallbackUsed = true
                        rawVideo
                    }

                    withContext(Dispatchers.Main) {
                        currentOutputFile = resultFile
                        onRecordFinished?.invoke(resultFile)
                        onRecordFinished = null
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Unexpected error in A/V mux, falling back to raw video", e)
                    isMuxFallbackUsed = true
                    withContext(Dispatchers.Main) {
                        currentOutputFile = rawVideo
                        onRecordFinished?.invoke(rawVideo)
                        onRecordFinished = null
                    }
                }
            }
        } else {
            // Không có file pcm hoặc video tạm, fallback an toàn
            Log.w(TAG, "Missing PCM or raw video for muxing. Fallback to raw video.")
            isMuxFallbackUsed = true
            val fallback = rawVideo ?: target
            currentOutputFile = fallback
            onRecordFinished?.invoke(fallback)
            onRecordFinished = null
        }
    }
}
