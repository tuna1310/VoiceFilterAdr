package com.antigravity.noichuan.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

/** Trạng thái video controller — được truyền vào / ra từ ChallengeScreen */
@Stable
class VideoRecorderController {
    internal var videoCapture: VideoCapture<Recorder>? = null
    internal var activeRecording: Recording? = null

    /** Bắt đầu quay. Trả về File sẽ lưu, hoặc null nếu chưa sẵn sàng. */
    fun startRecording(context: Context, onStart: () -> Unit, onError: (String) -> Unit): File? {
        val vc = videoCapture ?: run {
            onError("Camera is not ready to record")
            return null
        }
        val dir = File(context.filesDir, "videos").also { it.mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val file = File(dir, "noichuan_$name.mp4")

        activeRecording = vc.output
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStart()
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            onError("Video recording error: ${event.cause?.message}")
                        }
                    }
                    else -> {}
                }
            }
        return file
    }

    /** Dừng quay, gọi callback với đường dẫn file khi hoàn tất. */
    fun stopRecording(onFinalized: (String?) -> Unit) {
        val rec = activeRecording ?: run {
            onFinalized(null)
            return
        }
        // Lắng nghe VideoRecordEvent.Finalize để lấy đường dẫn thực
        // (đã set trong startRecording; chỉ cần stop ở đây)
        rec.stop()
        activeRecording = null
        onFinalized(null) // path đã được set khi prepareRecording
    }
}

@Composable
fun rememberVideoRecorderController(): VideoRecorderController {
    return remember { VideoRecorderController() }
}

@Composable
fun CameraPreview(
    isFrontCamera: Boolean = true,
    isFlashOn: Boolean = false,
    controller: VideoRecorderController? = null,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, isFrontCamera, isFlashOn) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val cameraSelector = if (isFrontCamera) {
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                        CameraSelector.DEFAULT_BACK_CAMERA
                    else CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider.unbindAll()

                if (controller != null) {
                    // Quay video: bind cả Preview + VideoCapture
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HD,
                                androidx.camera.video.FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                            )
                        )
                        .build()
                    val videoCapture = VideoCapture.withOutput(recorder)
                    controller.videoCapture = videoCapture

                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isFlashOn)
                    }
                } else {
                    // Preview only
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isFlashOn)
                    }
                }
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Lỗi khởi tạo camera", exc)
                onError("Could not start camera: ${exc.message}")
            }
        }, mainExecutor)

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
