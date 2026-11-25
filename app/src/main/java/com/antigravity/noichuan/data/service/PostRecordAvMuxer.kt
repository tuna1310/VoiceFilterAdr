package com.antigravity.noichuan.data.service

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.antigravity.noichuan.domain.service.AvSyncPlan
import com.antigravity.noichuan.domain.service.MuxResult
import com.antigravity.noichuan.domain.service.MuxerOffsetCalculator
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

import android.content.Context
import com.antigravity.noichuan.R
import com.antigravity.noichuan.domain.game.GameEventTimeline
import com.antigravity.noichuan.domain.service.TimelineAudioMixer

/**
 * Trình ghép Audio - Video hậu kỳ (Post-Record A/V Muxer) theo Phương án (b).
 * 
 * NGUYÊN TẮC AN TOÀN TUYỆT ĐỐI:
 * Nếu quá trình mã hóa AAC hoặc ghép track xảy ra sự cố, phương thức luôn trả về
 * file video gốc (không tiếng) và thông báo lỗi rõ ràng. KHÔNG BAO GIỜ làm mất file video của người chơi.
 */
class PostRecordAvMuxer {

    companion object {
        private const val TAG = "PostRecordAvMuxer"
        private const val AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val SAMPLE_RATE = 16000
        private const val BIT_RATE = 64000
    }

    /**
     * Thực hiện ghép video không tiếng và file PCM thành file MP4 hoàn chỉnh.
     * Tự động trộn SFX từ GameEventTimeline (nếu được cung cấp) trước khi mã hóa AAC.
     */
    fun mux(
        videoOnlyFile: File,
        pcmAudioFile: File?,
        videoStartNanos: Long,
        audioStartNanos: Long,
        outputMuxedFile: File,
        timeline: GameEventTimeline? = null,
        context: Context? = null
    ): MuxResult {
        if (!videoOnlyFile.exists() || videoOnlyFile.length() <= 0) {
            return MuxResult(
                finalVideoFile = videoOnlyFile,
                isAudioMuxed = false,
                syncPlan = null,
                errorMessage = "Source video file is missing or empty."
            )
        }

        if (pcmAudioFile == null || !pcmAudioFile.exists() || pcmAudioFile.length() <= 0) {
            Log.w(TAG, "PCM audio file is missing or empty, keeping video-only clip.")
            return MuxResult(
                finalVideoFile = videoOnlyFile,
                isAudioMuxed = false,
                syncPlan = null,
                errorMessage = "No audio captured. Video saved without audio."
            )
        }

        var effectivePcmFile = pcmAudioFile
        var tempMixedFile: File? = null

        // Trộn SFX từ GameEventTimeline vào PCM nếu có
        if (timeline != null && context != null && timeline.events.isNotEmpty()) {
            try {
                val dingBytes = try { context.resources.openRawResource(R.raw.ding).use { it.readBytes() } } catch (e: Exception) { null }
                val buzzBytes = try { context.resources.openRawResource(R.raw.buzz).use { it.readBytes() } } catch (e: Exception) { null }
                val crashBytes = try { context.resources.openRawResource(R.raw.crash).use { it.readBytes() } } catch (e: Exception) { null }
                val mixTarget = File(pcmAudioFile.parentFile, "timeline_mixed_${System.currentTimeMillis()}.pcm")
                effectivePcmFile = TimelineAudioMixer().mixTimeline(
                    micPcmFile = pcmAudioFile,
                    timeline = timeline,
                    dingWav = dingBytes,
                    buzzWav = buzzBytes,
                    crashWav = crashBytes,
                    outputFile = mixTarget
                )
                if (effectivePcmFile != pcmAudioFile) {
                    tempMixedFile = effectivePcmFile
                    Log.d(TAG, "Timeline audio mixing applied with ${timeline.events.size} events")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Timeline SFX mixing failed, proceeding with original PCM audio", e)
                effectivePcmFile = pcmAudioFile
            }
        }

        var syncPlan: AvSyncPlan? = null
        try {
            // 1. Trích xuất thời lượng từ video gốc
            val extractor = MediaExtractor()
            extractor.setDataSource(videoOnlyFile.absolutePath)
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                extractor.release()
                return MuxerOffsetCalculator.createFallbackResult(
                    videoOnlyFile,
                    "No valid video track found in recording"
                )
            }

            val videoDurationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                videoFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val videoDurationMs = videoDurationUs / 1000L

            // 2. Tính toán thời lượng audio từ dung lượng PCM (16kHz, 16-bit mono = 32000 bytes/sec)
            val audioBytes = effectivePcmFile.length()
            val audioDurationMs = (audioBytes * 1000L) / (SAMPLE_RATE * 2L)

            // 3. Tính toán Sync Plan
            syncPlan = MuxerOffsetCalculator.calculateSyncPlan(
                videoStartNanos = videoStartNanos,
                audioStartNanos = audioStartNanos,
                videoDurationMs = videoDurationMs,
                audioDurationMs = audioDurationMs
            )
            Log.d(TAG, "Sync plan: offset=${syncPlan.offsetMs}ms, trim=${syncPlan.trimAudioMs}ms, pad=${syncPlan.padSilenceMs}ms, drift=${syncPlan.finalDriftMs}ms")

            // 4. Mã hóa PCM thành AAC và mux cùng Video Track
            encodeAndMuxInternal(
                extractor = extractor,
                videoTrackIndex = videoTrackIndex,
                videoFormat = videoFormat,
                pcmFile = effectivePcmFile,
                syncPlan = syncPlan,
                outputFile = outputMuxedFile
            )

            extractor.release()

            if (outputMuxedFile.exists() && outputMuxedFile.length() > 0) {
                return MuxResult(
                    finalVideoFile = outputMuxedFile,
                    isAudioMuxed = true,
                    syncPlan = syncPlan,
                    errorMessage = null
                )
            } else {
                return MuxerOffsetCalculator.createFallbackResult(
                    videoOnlyFile,
                    "Output file is empty after muxing",
                    syncPlan
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong quá trình Muxer video & audio", e)
            return MuxerOffsetCalculator.createFallbackResult(
                videoOnlyFile,
                e.message ?: "Unknown exception",
                syncPlan
            )
        } finally {
            tempMixedFile?.delete()
        }
    }

    private fun encodeAndMuxInternal(
        extractor: MediaExtractor,
        videoTrackIndex: Int,
        videoFormat: MediaFormat,
        pcmFile: File,
        syncPlan: AvSyncPlan,
        outputFile: File
    ) {
        outputFile.parentFile?.mkdirs()
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // Add Video Track
        val muxerVideoTrack = muxer.addTrack(videoFormat)

        // Setup AAC Audio Encoder
        val audioFormat = MediaFormat.createAudioFormat(AAC_MIME, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val encoder = MediaCodec.createEncoderByType(AAC_MIME)
        encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var muxerAudioTrack = -1
        var muxerStarted = false

        val pcmInputStream = FileInputStream(pcmFile)

        // Bỏ qua (trim) đoạn audio dôi dư ở đầu nếu offset > 0
        if (syncPlan.trimAudioMs > 0) {
            val skipBytes = (syncPlan.trimAudioMs * SAMPLE_RATE * 2L) / 1000L
            pcmInputStream.skip(skipBytes)
        }

        val inputBuffer = ByteArray(4096)
        var isPcmEos = false
        var audioPtsUs = 0L

        // Chèn padding silence nếu audio bắt đầu sau video
        if (syncPlan.padSilenceMs > 0) {
            audioPtsUs += syncPlan.padSilenceMs * 1000L
        }

        try {
            // Pump audio encoding loop
            var encodingFinished = false
            while (!encodingFinished) {
                if (!isPcmEos) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10000L)
                    if (inputBufferIndex >= 0) {
                        val codecInputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        codecInputBuffer?.clear()

                        val bytesRead = pcmInputStream.read(inputBuffer)
                        if (bytesRead > 0) {
                            codecInputBuffer?.put(inputBuffer, 0, bytesRead)
                            val pts = audioPtsUs
                            encoder.queueInputBuffer(inputBufferIndex, 0, bytesRead, pts, 0)
                            audioPtsUs += (bytesRead * 1_000_000L) / (SAMPLE_RATE * 2L)
                        } else {
                            isPcmEos = true
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                audioPtsUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        }
                    }
                }

                // Drain encoded AAC buffers
                var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                while (outputIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (!muxerStarted && outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Handled below
                    } else if (muxerStarted && bufferInfo.size > 0) {
                        val encodedData = encoder.getOutputBuffer(outputIndex)
                        if (encodedData != null) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerAudioTrack, encodedData, bufferInfo)
                        }
                    }

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encodingFinished = true
                        encoder.releaseOutputBuffer(outputIndex, false)
                        break
                    }

                    encoder.releaseOutputBuffer(outputIndex, false)
                    outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                }

                if (!muxerStarted && outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newAudioFormat = encoder.outputFormat
                    muxerAudioTrack = muxer.addTrack(newAudioFormat)
                    muxer.start()
                    muxerStarted = true

                    // Write video samples into muxer
                    extractor.selectTrack(videoTrackIndex)
                    val videoByteBuffer = ByteBuffer.allocateDirect(1024 * 1024)
                    val videoBufferInfo = MediaCodec.BufferInfo()

                    while (true) {
                        val sampleSize = extractor.readSampleData(videoByteBuffer, 0)
                        if (sampleSize < 0) break

                        videoBufferInfo.offset = 0
                        videoBufferInfo.size = sampleSize
                        videoBufferInfo.presentationTimeUs = extractor.sampleTime
                        videoBufferInfo.flags = extractor.sampleFlags

                        muxer.writeSampleData(muxerVideoTrack, videoByteBuffer, videoBufferInfo)
                        extractor.advance()
                    }
                }
            }

        } finally {
            try {
                pcmInputStream.close()
            } catch (_: Exception) {}
            try {
                encoder.stop()
                encoder.release()
            } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer.stop()
                }
                muxer.release()
            } catch (_: Exception) {}
        }
    }
}
