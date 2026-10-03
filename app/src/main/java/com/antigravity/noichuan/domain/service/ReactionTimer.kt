package com.antigravity.noichuan.domain.service

import com.antigravity.noichuan.data.model.DifficultyLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dem nguoc thoi gian phan ung cho moi tu trong mini-game.
 *
 * Thoi gian theo spec (chua do thuc te):
 *   BEGINNER     = 3000ms
 *   INTERMEDIATE = 2500ms
 *   EXPERT       = 2000ms
 *
 * @param scope         CoroutineScope tu ViewModel (viewModelScope)
 * @param onTimeout     Callback khi het gio; goi tren coroutine context cua [scope]
 * @param tickIntervalMs Khoang cach giua moi lan cap nhat progress (mac dinh 50ms)
 */
class ReactionTimer(
    private val scope: CoroutineScope,
    private val onTimeout: () -> Unit,
    private val tickIntervalMs: Long = TICK_INTERVAL_MS
) {
    companion object {
        const val TICK_INTERVAL_MS = 50L

        /** Thoi gian gioi han theo do kho (ms); theo spec, chua do tren nguoi dung that. */
        fun limitMsForLevel(level: DifficultyLevel): Long = when (level) {
            DifficultyLevel.BEGINNER     -> 3_000L
            DifficultyLevel.INTERMEDIATE -> 2_500L
            DifficultyLevel.EXPERT       -> 2_000L
        }
    }

    private val _progress = MutableStateFlow(0f)
    /** Progress 0.0 (bat dau) den 1.0 (het gio). */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Bat dau dem nguoc cho level duoc chi dinh.
     * Neu co timer dang chay, huy truoc roi bat dau moi.
     */
    fun start(level: DifficultyLevel) {
        start(overrideLimitMs = limitMsForLevel(level))
    }

    /**
     * Bat dau dem nguoc voi thoi gian tuy chinh (dung cho test).
     */
    fun start(overrideLimitMs: Long) {
        timerJob?.cancel()
        _progress.value = 0f

        timerJob = scope.launch {
            var elapsedMs = 0L

            while (elapsedMs < overrideLimitMs) {
                val stepMs = minOf(tickIntervalMs, overrideLimitMs - elapsedMs)
                delay(stepMs)
                elapsedMs += stepMs
                val prog = (elapsedMs.toFloat() / overrideLimitMs).coerceIn(0f, 1f)
                _progress.value = prog
            }

            onTimeout()
        }
    }

    /** Huy timer hien tai (khong reset progress). Dung khi nguoi choi tra loi dung. */
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }

    /** Huy timer va dat lai progress ve 0. Dung khi chuyen tu moi hoac ket thuc. */
    fun reset() {
        cancel()
        _progress.value = 0f
    }

    val isRunning: Boolean
        get() = timerJob?.isActive == true
}
