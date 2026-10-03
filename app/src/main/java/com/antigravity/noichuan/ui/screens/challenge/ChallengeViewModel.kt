package com.antigravity.noichuan.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.data.model.Word
import com.antigravity.noichuan.domain.game.CategoryRushEvaluator
import com.antigravity.noichuan.domain.game.CategoryRushGameMode
import com.antigravity.noichuan.domain.game.DifficultyConfig
import com.antigravity.noichuan.domain.game.GameAction
import com.antigravity.noichuan.domain.game.GameMode
import com.antigravity.noichuan.domain.game.GameModeRegistry
import com.antigravity.noichuan.domain.game.GameOverReason
import com.antigravity.noichuan.domain.game.GameState
import com.antigravity.noichuan.domain.game.GameStateMachine
import com.antigravity.noichuan.domain.game.TongueTwisterEvaluator
import com.antigravity.noichuan.domain.game.TongueTwisterGameMode
import com.antigravity.noichuan.domain.game.VoiceRunnerGameMode
import com.antigravity.noichuan.domain.repository.ProgressRepository
import com.antigravity.noichuan.domain.repository.WordRepository
import com.antigravity.noichuan.domain.service.PronunciationMatcher
import com.antigravity.noichuan.domain.service.ReactionTimer
import com.antigravity.noichuan.domain.service.SpeechEngineEvent
import com.antigravity.noichuan.domain.service.SpeechRecognizerEngine
import com.antigravity.noichuan.domain.service.TtsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordingUiState(
    val words: List<Word> = emptyList(),
    val currentWordIndex: Int = 0,
    val subject: Subject = Subject.COLORS,
    val level: DifficultyLevel = DifficultyLevel.BEGINNER,
    val selectedDurationSeconds: Int = 60, // 15, 60, 600 (10m)
    val isRecording: Boolean = false,
    val elapsedSeconds: Float = 0f,
    val gateProgress: Float = 0f, // 0f (xa) -> 1f (ở chỗ nhân vật) -> 1.3f (vượt qua)
    val isFrontCamera: Boolean = true,
    val isFlashOn: Boolean = false,
    val isSkinFilterOn: Boolean = false,
    val isLipsFilterOn: Boolean = false,
    val recognizedSpokenText: String = "",
    val isListening: Boolean = false,
    val statusMessage: String? = null,
    val showConfetti: Boolean = false,
    val isGateSuccess: Boolean = false,
    val consecutiveCorrect: Int = 0,
    val successBannerText: String? = null,
    val wordTimerProgress: Float = 0f,
    val lastMatchReason: String = "",
    val gameMode: String = "voice_runner",
    val requestedWordId: String? = null,
    val emptyWordsError: String? = null,
    // Voice Runner & Game State Machine (A3)
    val gameState: GameState = GameState.Idle,
    val livesRemaining: Int = 3,
    val maxLives: Int = 3,
    val score: Int = 0,
    val combo: Int = 0,
    val isPaused: Boolean = false,
    val countdownRemaining: Int? = null,
    val timelineJson: String = "[]",
    val highlightedWordIndices: Set<Int> = emptySet(),
    val uniqueWordsCount: Int = 0,
    val gameModeDisplayName: String = "Voice Runner",
    val tongueTwisterFeedback: String? = null,
    val isSfxEnabled: Boolean = true,
    val isBgmEnabled: Boolean = true,
    val isWordSuccess: Boolean = false,
    val isWordFailure: Boolean = false,
    val hapticTick: Long = 0L
) {
    val currentWord: Word? get() = words.getOrNull(currentWordIndex)
    val nextWord: Word? get() = words.getOrNull(currentWordIndex + 1)
    val progressPercent: Float get() = (elapsedSeconds / selectedDurationSeconds).coerceIn(0f, 1f)
}

sealed interface RecordingNavEvent {
    data class NavigateToPreview(
        val videoPath: String?,
        val subjectKey: String,
        val levelKey: String,
        val durationSec: Int,
        val wordsCompleted: Int,
        val totalScore: Int = 0,
        val stars: Int = 1,
        val maxCombo: Int = 0,
        val timelineJson: String = "[]"
    ) : RecordingNavEvent
}

class ChallengeViewModel(
    val subjectKey: String,
    val levelKey: String,
    val modeKey: String,
    val requestedWordId: String?,
    private val wordRepository: WordRepository,
    val speechEngine: SpeechRecognizerEngine,
    private val ttsService: TtsService,
    private val scoreCalculator: com.antigravity.noichuan.domain.service.ScoreCalculator,
    private val progressRepository: ProgressRepository,
    private val soundEffectManager: com.antigravity.noichuan.data.service.SoundEffectManager? = null
) : ViewModel() {

    companion object {
        private const val TAG = "ChallengeViewModel"
    }

    private fun logInfo(message: String) = println("I/$TAG: $message")
    private fun logWarning(message: String) = println("W/$TAG: $message")
    private fun logError(message: String) = println("E/$TAG: $message")

    private val subject = Subject.fromKey(subjectKey)
    private val level = DifficultyLevel.fromKey(levelKey)

    val activeGameMode: GameMode = GameModeRegistry.fromKey(modeKey)
    val diffConfig: DifficultyConfig = DifficultyConfig.forLevel(level)

    val gameStateMachine = GameStateMachine()

    private var categoryRushEvaluator: CategoryRushEvaluator? = null
    private val tongueTwisterEvaluator = TongueTwisterEvaluator()
    private val pausesBetweenWordsMs = mutableListOf<Long>()
    private var lastWordTimeMs = 0L

    private val _uiState = MutableStateFlow(
        RecordingUiState(
            subject = subject,
            level = level,
            gameMode = modeKey,
            gameModeDisplayName = activeGameMode.displayName,
            requestedWordId = requestedWordId
        )
    )
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<RecordingNavEvent>()
    val navEvents: SharedFlow<RecordingNavEvent> = _navEvents.asSharedFlow()

    private val matcher = PronunciationMatcher()

    private var gameLoopJob: Job? = null
    private var countdownJob: Job? = null
    private var recordedVideoPathHolder: String? = null

    init {
        speechEngine.initialize(
            onReady = { logInfo("SpeechRecognizerEngine (${speechEngine.engineType}) initialized successfully") },
            onError = { err -> logWarning("SpeechRecognizerEngine error: $err") }
        )

        loadWords()
        observeSpeechRecognition()
        observeGameStateMachine()
    }

    private fun loadWords() {
        viewModelScope.launch {
            val list = if (activeGameMode is TongueTwisterGameMode) {
                getTongueTwistersForLevel(level)
            } else {
                val dbList = wordRepository.getWordsBySubjectAndLevel(subject.key, level.key)
                if (activeGameMode is CategoryRushGameMode) {
                    dbList
                } else {
                    // Voice Runner: lọc theo độ dài từ cấu hình DifficultyConfig
                    val filtered = dbList.filter { it.word.length in diffConfig.minWordLength..diffConfig.maxWordLength }
                    filtered.ifEmpty { dbList }
                }
            }

            val orderedList = if (activeGameMode is TongueTwisterGameMode) {
                list
            } else {
                orderWordsForRequestedStart(list, requestedWordId)
            }

            if (activeGameMode is CategoryRushGameMode) {
                categoryRushEvaluator = CategoryRushEvaluator(orderedList.map { it.word })
            }

            logInfo(
                "Challenge route: subject=${subject.key}, level=${level.key}, wordId=${requestedWordId ?: "_"}, mode=$modeKey, words=${orderedList.size}, start=${orderedList.firstOrNull()?.id ?: "none"}"
            )

            if (orderedList.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    words = orderedList,
                    currentWordIndex = 0,
                    emptyWordsError = null
                )
                return@launch
            }

            val error = "Không có từ cho subject=${subject.key}, level=${level.key}."
            logError(error)
            if (subject == Subject.COLORS) {
                logWarning("Using Colors-only local fallback for empty Colors list.")
                val defaults = getDefaultWords()
                if (activeGameMode is CategoryRushGameMode) {
                    categoryRushEvaluator = CategoryRushEvaluator(defaults.map { it.word })
                }
                _uiState.value = _uiState.value.copy(
                    words = defaults,
                    currentWordIndex = 0,
                    emptyWordsError = error
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    words = emptyList(),
                    currentWordIndex = 0,
                    statusMessage = error,
                    emptyWordsError = error
                )
            }
        }
    }

    private fun getTongueTwistersForLevel(level: DifficultyLevel): List<Word> {
        return when (level) {
            DifficultyLevel.BEGINNER -> listOf(
                Word("tt_b1", "I scream you scream we all scream for ice cream", "/aɪ skriːm/", "Đọc rõ từng từ scream và cream", "basic", "tongue_twister"),
                Word("tt_b2", "Red lorry yellow lorry", "/rɛd ˈlɒri ˈjɛloʊ ˈlɒri/", "Phân biệt rõ âm R và L", "basic", "tongue_twister"),
                Word("tt_b3", "She sells seashells by the seashore", "/ʃiː sɛlz ˈsiːˌʃɛlz baɪ ðə ˈsiːˌʃɔːr/", "Phân biệt âm S nhẹ và SH nặng", "basic", "tongue_twister"),
                Word("tt_b4", "Blue bluebird on the blue branch", "/bluː ˈbluːˌbɜːrd ɒn ðə bluː bræntʃ/", "Bật âm B và L nhanh dứt khoát", "basic", "tongue_twister")
            )
            DifficultyLevel.INTERMEDIATE -> listOf(
                Word("tt_i1", "How can a clam cram in a clean cream can", "/haʊ kæn ə klæm kræm ɪn ə kliːn kriːm kæn/", "Uốn lưỡi chuẩn giữa CL và CR", "intermediate", "tongue_twister"),
                Word("tt_i2", "Peter Piper picked a peck of pickled peppers", "/ˈpiːtər ˈpaɪpər pɪkt ə pɛk ɒv ˈpɪkld ˈpɛpərz/", "Bật hơi âm P mạnh và rõ", "intermediate", "tongue_twister"),
                Word("tt_i3", "Fuzzy Wuzzy was a bear Fuzzy Wuzzy had no hair", "/ˈfʌzi ˈwʌzi wɒz ə bɛər ˈfʌzi ˈwʌzi hæd noʊ hɛər/", "Luyện âm Z rung thanh quản", "intermediate", "tongue_twister"),
                Word("tt_i4", "Six slippery snails slid slowly seaward", "/sɪks ˈslɪpəri sneɪlz slɪd ˈsloʊli ˈsiːwərd/", "Giữ luồng hơi âm S và SL liền mạch", "intermediate", "tongue_twister")
            )
            DifficultyLevel.EXPERT -> listOf(
                Word("tt_e1", "Pad kid pour curd pull cod", "/pæd kɪd pɔːr kɜːrd pʊl kɒd/", "Đảo nhanh giữa P và K không dính âm", "expert", "tongue_twister"),
                Word("tt_e2", "The thirty three thieves thought that they thrilled the throne", "/ðə ˈθɜːti θriː θiːvz θɔːt ðæt ðeɪ θrɪld ðə θroʊn/", "Luyện âm TH vô thanh và hữu thanh", "expert", "tongue_twister"),
                Word("tt_e3", "Betty Botter bought some butter but she said the butter is bitter", "/ˈbɛti ˈbɒtər bɔːt sʌm ˈbʌtər/", "Luyện âm T và B tốc độ cao", "expert", "tongue_twister"),
                Word("tt_e4", "Selfish shellfish should seldom sell selfish shellfish", "/ˈsɛlfɪʃ ˈʃɛlˌfɪʃ ʃʊd ˈsɛldəm sɛl/", "Chuyển liên tục giữa S và SH", "expert", "tongue_twister")
            )
        }
    }

    private fun orderWordsForRequestedStart(words: List<Word>, requestedWordId: String?): List<Word> {
        if (requestedWordId.isNullOrBlank()) return words
        val index = words.indexOfFirst { it.id == requestedWordId }
        if (index < 0) {
            logWarning("Requested wordId=$requestedWordId not found inside subject=${subject.key}, level=${level.key}; using normal order.")
            return words
        }
        return words.drop(index) + words.take(index)
    }

    private fun observeGameStateMachine() {
        viewModelScope.launch {
            var previousState: GameState? = null
            var previousLives: Int = 3
            var wasSuccess = false

            gameStateMachine.state.collect { state ->
                when (state) {
                    is GameState.Idle -> {
                        previousState = state
                        previousLives = 3
                        wasSuccess = false
                        _uiState.value = _uiState.value.copy(
                            gameState = state,
                            isRecording = false,
                            countdownRemaining = null,
                            isPaused = false
                        )
                    }
                    is GameState.Countdown -> {
                        previousState = state
                        _uiState.value = _uiState.value.copy(
                            gameState = state,
                            isRecording = true,
                            countdownRemaining = state.secondsRemaining,
                            isPaused = false
                        )
                    }
                    is GameState.Playing -> {
                        if (previousState !is GameState.Playing) {
                            soundEffectManager?.startBgm()
                        }

                        // Cập nhật Grammar cho Speech Engine khi chuyển từ mới
                        if (state.currentWordIndex != _uiState.value.currentWordIndex || _uiState.value.words.isEmpty()) {
                            val grammar = activeGameMode.buildGrammar(_uiState.value.words, state.currentWord)
                            speechEngine.setGrammar(grammar)
                            pausesBetweenWordsMs.clear()
                            lastWordTimeMs = 0L
                            _uiState.value = _uiState.value.copy(highlightedWordIndices = emptySet())
                        }

                        val isSuccessNow = state.isGateSuccess == true
                        val lifeLostNow = state.livesRemaining < previousLives

                        if (isSuccessNow && !wasSuccess) {
                            soundEffectManager?.playDing()
                        } else if (lifeLostNow) {
                            soundEffectManager?.playBuzz()
                        }

                        wasSuccess = isSuccessNow
                        previousLives = state.livesRemaining
                        previousState = state

                        _uiState.value = _uiState.value.copy(
                            gameState = state,
                            words = state.words,
                            currentWordIndex = state.currentWordIndex,
                            livesRemaining = state.livesRemaining,
                            maxLives = state.maxLives,
                            score = state.score,
                            combo = state.combo,
                            gateProgress = state.gateProgress,
                            wordTimerProgress = state.wordTimerProgress,
                            elapsedSeconds = state.elapsedSeconds,
                            isGateSuccess = isSuccessNow,
                            showConfetti = isSuccessNow,
                            isWordSuccess = isSuccessNow,
                            isWordFailure = lifeLostNow,
                            hapticTick = if (isSuccessNow || lifeLostNow) System.currentTimeMillis() else _uiState.value.hapticTick,
                            consecutiveCorrect = state.consecutiveCorrect,
                            successBannerText = state.bannerMessage,
                            countdownRemaining = null,
                            isPaused = false,
                            isRecording = true
                        )
                    }
                    is GameState.Paused -> {
                        soundEffectManager?.pauseBgm()
                        previousState = state
                        _uiState.value = _uiState.value.copy(
                            gameState = state,
                            isPaused = true
                        )
                    }
                    is GameState.Finished -> {
                        soundEffectManager?.stopBgm()
                        if (state.reason == GameOverReason.OUT_OF_LIVES) {
                            soundEffectManager?.playCrash()
                        }
                        handleGameFinished(state)
                    }
                }
            }
        }
    }

    private fun observeSpeechRecognition() {
        viewModelScope.launch {
            speechEngine.events.collect { event ->
                when (event) {
                    is SpeechEngineEvent.Listening -> {
                        _uiState.value = _uiState.value.copy(isListening = true)
                    }
                    is SpeechEngineEvent.PartialResult -> {
                        val text = event.text.trim()
                        if (text.isNotBlank()) {
                            _uiState.value = _uiState.value.copy(recognizedSpokenText = text)
                            checkCandidateAndAdvance(text)
                        }
                    }
                    is SpeechEngineEvent.FinalResult -> {
                        val text = event.text.trim()
                        if (text.isNotBlank()) {
                            _uiState.value = _uiState.value.copy(
                                recognizedSpokenText = text,
                                isListening = false
                            )
                            checkCandidateAndAdvance(text)
                        }
                    }
                    is SpeechEngineEvent.NoSpeechTimeout -> {
                        // SilenceWatchdog ngắt khi im lặng quá 4s: tính như 1 lần timeout, không loop vô hạn
                        _uiState.value = _uiState.value.copy(isListening = false)
                        gameStateMachine.processAction(GameAction.WordTimeout)
                    }
                    is SpeechEngineEvent.Error -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkCandidateAndAdvance(spokenText: String) {
        val playing = _uiState.value.gameState as? GameState.Playing ?: return
        val currentWord = playing.currentWord ?: return

        when (activeGameMode) {
            is CategoryRushGameMode -> {
                val eval = categoryRushEvaluator ?: return
                when (val res = eval.submitWord(spokenText)) {
                    is CategoryRushEvaluator.SubmissionResult.Accepted -> {
                        _uiState.value = _uiState.value.copy(
                            uniqueWordsCount = res.totalUniqueCount,
                            score = eval.getScore(),
                            statusMessage = "Đúng: ${res.word} (+10đ)",
                            lastMatchReason = "accepted"
                        )
                        gameStateMachine.processAction(GameAction.WordMatched(spokenText, pointsAwarded = 10))
                    }
                    is CategoryRushEvaluator.SubmissionResult.Duplicate -> {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "Từ '${res.word}' đã được nói rồi!",
                            lastMatchReason = "duplicate"
                        )
                    }
                    is CategoryRushEvaluator.SubmissionResult.NotInSubject -> {
                        _uiState.value = _uiState.value.copy(lastMatchReason = "not_in_subject")
                    }
                }
            }
            is TongueTwisterGameMode -> {
                val targetTokens = tongueTwisterEvaluator.tokenizeSentence(currentWord.word)
                val spokenTokens = tongueTwisterEvaluator.tokenizeSentence(spokenText)
                val matched = tongueTwisterEvaluator.calculateMatchedIndices(targetTokens, spokenTokens)

                val now = System.currentTimeMillis()
                if (lastWordTimeMs > 0L) {
                    pausesBetweenWordsMs.add(now - lastWordTimeMs)
                }
                lastWordTimeMs = now

                _uiState.value = _uiState.value.copy(
                    highlightedWordIndices = matched,
                    lastMatchReason = "matched_${matched.size}_of_${targetTokens.size}"
                )

                if (matched.size == targetTokens.size && targetTokens.isNotEmpty()) {
                    val evalResult = tongueTwisterEvaluator.evaluate(
                        targetSentence = currentWord.word,
                        spokenText = spokenText,
                        pausesBetweenWordsMs = pausesBetweenWordsMs,
                        completionDurationSec = playing.elapsedSeconds
                    )
                    _uiState.value = _uiState.value.copy(
                        statusMessage = evalResult.feedback,
                        tongueTwisterFeedback = evalResult.feedback
                    )
                    gameStateMachine.processAction(GameAction.WordMatched(spokenText, pointsAwarded = evalResult.totalScore))
                }
            }
            else -> {
                val result = matcher.checkCandidates(
                    target = currentWord.word,
                    candidates = listOf(spokenText),
                    level = level,
                    acceptedVariants = currentWord.acceptedVariants
                )

                _uiState.value = _uiState.value.copy(lastMatchReason = result.reason)
                if (result.matched) {
                    gameStateMachine.processAction(GameAction.WordMatched(spokenText, pointsAwarded = 10))
                }
            }
        }
    }

    fun selectDuration(seconds: Int) {
        if (!_uiState.value.isRecording) {
            _uiState.value = _uiState.value.copy(selectedDurationSeconds = seconds)
        }
    }

    fun toggleCameraFacing() {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(isFlashOn = !_uiState.value.isFlashOn)
    }

    fun toggleSkinFilter() {
        _uiState.value = _uiState.value.copy(
            isSkinFilterOn = !_uiState.value.isSkinFilterOn,
            statusMessage = if (!_uiState.value.isSkinFilterOn) "Skin filter enabled" else "Skin filter disabled"
        )
    }

    fun toggleLipsFilter() {
        _uiState.value = _uiState.value.copy(
            isLipsFilterOn = !_uiState.value.isLipsFilterOn,
            statusMessage = if (!_uiState.value.isLipsFilterOn) "Lips tint enabled" else "Lips tint disabled"
        )
    }

    fun toggleRecording(recordedVideoFilePath: String? = null) {
        recordedVideoPathHolder = recordedVideoFilePath
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val words = _uiState.value.words.ifEmpty { getDefaultWords() }
        val durationSec = activeGameMode.getDurationSeconds(_uiState.value.selectedDurationSeconds)
        val timeLimitMs = activeGameMode.getTimeLimitPerWordMs(diffConfig)

        if (activeGameMode is CategoryRushGameMode) {
            categoryRushEvaluator = CategoryRushEvaluator(words.map { it.word })
        }

        // Cài đặt Grammar ban đầu cho Vosk
        val initialGrammar = activeGameMode.buildGrammar(words, words.firstOrNull())
        speechEngine.setGrammar(initialGrammar)

        // Khởi động StateMachine với đếm ngược 3-2-1
        gameStateMachine.processAction(
            GameAction.Start(
                words = words,
                durationSeconds = durationSec,
                timeLimitPerWordMs = timeLimitMs
            )
        )

        // Chạy coroutine đếm ngược 3-2-1
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                gameStateMachine.processAction(GameAction.CountdownTick(sec))
                delay(1000L)
            }
            gameStateMachine.processAction(GameAction.CountdownFinish)
            // Bắt đầu game loop duy nhất và bật nhận diện giọng nói
            startGameLoop()
            speechEngine.startListening()
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && _uiState.value.isRecording) {
                delay(30L)
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now
                gameStateMachine.processAction(GameAction.Tick(delta))
            }
        }
    }

    fun pauseGame() {
        if (_uiState.value.isRecording && !_uiState.value.isPaused) {
            gameStateMachine.processAction(GameAction.Pause)
            speechEngine.stopListening()
        }
    }

    fun resumeGame() {
        if (_uiState.value.isPaused) {
            gameStateMachine.processAction(GameAction.Resume)
            speechEngine.startListening()
            soundEffectManager?.resumeBgm()
        }
    }

    fun toggleSfx() {
        val updated = soundEffectManager?.toggleSfx() ?: !_uiState.value.isSfxEnabled
        _uiState.value = _uiState.value.copy(isSfxEnabled = updated)
    }

    fun toggleBgm() {
        val updated = soundEffectManager?.toggleBgm() ?: !_uiState.value.isBgmEnabled
        _uiState.value = _uiState.value.copy(isBgmEnabled = updated)
    }

    fun quitGame() {
        stopRecording()
    }

    private fun stopRecording() {
        countdownJob?.cancel()
        gameLoopJob?.cancel()
        speechEngine.stopListening()

        gameStateMachine.processAction(GameAction.Finish(GameOverReason.USER_QUIT))
    }

    private fun handleGameFinished(finishedState: GameState.Finished) {
        countdownJob?.cancel()
        gameLoopJob?.cancel()
        speechEngine.stopListening()

        val summary = finishedState.summary
        val timelineJson = gameStateMachine.eventTimeline.toJsonString()

        _uiState.value = _uiState.value.copy(
            gameState = finishedState,
            isRecording = false,
            isListening = false,
            timelineJson = timelineJson
        )

        viewModelScope.launch {
            progressRepository.recordCompletion(
                wordId = summary.wordsCompleted.toString(),
                subject = subject.key,
                score = summary.totalScore,
                stars = summary.stars
            )

            _navEvents.emit(
                RecordingNavEvent.NavigateToPreview(
                    videoPath = recordedVideoPathHolder,
                    subjectKey = subject.key,
                    levelKey = level.key,
                    durationSec = summary.durationSeconds.toInt().coerceAtLeast(1),
                    wordsCompleted = summary.wordsCompleted,
                    totalScore = summary.totalScore,
                    stars = summary.stars,
                    maxCombo = summary.maxCombo,
                    timelineJson = timelineJson
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEffectManager?.stopBgm()
        countdownJob?.cancel()
        gameLoopJob?.cancel()
        speechEngine.stopListening()
        speechEngine.destroy()
        ttsService.shutdown()
    }

    private fun getDefaultWords(): List<Word> {
        return listOf(
            Word("w1", "White", "/waɪt/", "Âm W tròn môi, bật T", "basic", subject.key),
            Word("w2", "Brown", "/braʊn/", "Nguyên âm AW, khép N", "basic", subject.key),
            Word("w3", "Black", "/blæk/", "Âm A bẹt, bật K", "basic", subject.key),
            Word("w4", "Green", "/ɡriːn/", "Kéo dài I, khép N", "basic", subject.key),
            Word("w5", "Red", "/rɛd/", "Uốn lưỡi R, bật D", "basic", subject.key)
        )
    }
}
