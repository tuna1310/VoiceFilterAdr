package com.antigravity.noichuan.data.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.antigravity.noichuan.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trình quản lý hiệu ứng âm thanh (SFX) và nhạc nền (BGM) cho game.
 * 
 * Sử dụng SoundPool cho âm thanh tức thời độ trễ thấp (ding, buzz, crash).
 * Giới hạn âm lượng vừa phải để tránh âm thanh từ loa ngoài dội ngược vào microphone
 * gây nhiễu engine nhận diện giọng nói (Vosk / Google).
 */
class SoundEffectManager(private val context: Context) {

    companion object {
        private const val TAG = "SoundEffectManager"
        private const val DEFAULT_SFX_VOLUME = 0.6f
        private const val DEFAULT_BGM_VOLUME = 0.25f
    }

    private var soundPool: SoundPool? = null
    private var dingSoundId: Int = 0
    private var buzzSoundId: Int = 0
    private var crashSoundId: Int = 0

    private var bgmPlayer: MediaPlayer? = null

    private val _isSfxEnabled = MutableStateFlow(true)
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    private val _isBgmEnabled = MutableStateFlow(true)
    val isBgmEnabled: StateFlow<Boolean> = _isBgmEnabled.asStateFlow()

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.let { sp ->
                dingSoundId = sp.load(context, R.raw.ding, 1)
                buzzSoundId = sp.load(context, R.raw.buzz, 1)
                crashSoundId = sp.load(context, R.raw.crash, 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khởi tạo SoundPool", e)
        }
    }

    fun playDing() {
        if (!_isSfxEnabled.value) return
        try {
            soundPool?.play(dingSoundId, DEFAULT_SFX_VOLUME, DEFAULT_SFX_VOLUME, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể phát ding sfx", e)
        }
    }

    fun playBuzz() {
        if (!_isSfxEnabled.value) return
        try {
            soundPool?.play(buzzSoundId, DEFAULT_SFX_VOLUME, DEFAULT_SFX_VOLUME, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể phát buzz sfx", e)
        }
    }

    fun playCrash() {
        if (!_isSfxEnabled.value) return
        try {
            soundPool?.play(crashSoundId, DEFAULT_SFX_VOLUME, DEFAULT_SFX_VOLUME, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể phát crash sfx", e)
        }
    }

    fun startBgm() {
        if (!_isBgmEnabled.value) return
        if (bgmPlayer != null) {
            resumeBgm()
            return
        }

        try {
            bgmPlayer = MediaPlayer.create(context, R.raw.bgm)?.apply {
                isLooping = true
                setVolume(DEFAULT_BGM_VOLUME, DEFAULT_BGM_VOLUME)
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phát BGM", e)
        }
    }

    fun pauseBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi pause BGM", e)
        }
    }

    fun resumeBgm() {
        if (!_isBgmEnabled.value) return
        try {
            if (bgmPlayer?.isPlaying == false) {
                bgmPlayer?.start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi resume BGM", e)
        }
    }

    fun stopBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi stop BGM", e)
            bgmPlayer = null
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        _isSfxEnabled.value = enabled
    }

    fun setBgmEnabled(enabled: Boolean) {
        _isBgmEnabled.value = enabled
        if (!enabled) {
            pauseBgm()
        } else {
            resumeBgm()
        }
    }

    fun toggleSfx(): Boolean {
        val newVal = !_isSfxEnabled.value
        setSfxEnabled(newVal)
        return newVal
    }

    fun toggleBgm(): Boolean {
        val newVal = !_isBgmEnabled.value
        setBgmEnabled(newVal)
        return newVal
    }

    fun release() {
        stopBgm()
        soundPool?.release()
        soundPool = null
    }
}
