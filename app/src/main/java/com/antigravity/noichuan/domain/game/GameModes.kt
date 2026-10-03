package com.antigravity.noichuan.domain.game

import com.antigravity.noichuan.data.model.Word
import java.util.Locale

/**
 * Interface GameMode chung cho tat ca che do choi (Voice Runner, Tongue Twister, Category Rush).
 */
interface GameMode {
    val key: String
    val displayName: String
    fun getDurationSeconds(configuredDuration: Int): Int
    fun getTimeLimitPerWordMs(difficultyConfig: DifficultyConfig): Long
    fun buildGrammar(words: List<Word>, currentWord: Word?): List<String>
}

class VoiceRunnerGameMode : GameMode {
    override val key: String = "voice_runner"
    override val displayName: String = "Voice Runner"
    override fun getDurationSeconds(configuredDuration: Int): Int = configuredDuration
    override fun getTimeLimitPerWordMs(difficultyConfig: DifficultyConfig): Long = difficultyConfig.timeLimitPerWordMs
    override fun buildGrammar(words: List<Word>, currentWord: Word?): List<String> {
        val list = mutableListOf<String>()
        currentWord?.let {
            list.add(it.word)
            list.addAll(it.acceptedVariants)
        }
        return list.distinct()
    }
}

class TongueTwisterGameMode : GameMode {
    override val key: String = "tongue_twister"
    override val displayName: String = "Tongue Twister"
    override fun getDurationSeconds(configuredDuration: Int): Int = configuredDuration.coerceAtLeast(30)
    override fun getTimeLimitPerWordMs(difficultyConfig: DifficultyConfig): Long = 60000L
    override fun buildGrammar(words: List<Word>, currentWord: Word?): List<String> {
        val list = mutableListOf<String>()
        currentWord?.let { cw ->
            val tokens = cw.word.split("\\s+".toRegex())
                .map { it.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "") }
                .filter { it.isNotBlank() }
            list.addAll(tokens)
            list.add("[unk]")
        }
        return list.distinct()
    }
}

class CategoryRushGameMode : GameMode {
    override val key: String = "category_rush"
    override val displayName: String = "Category Rush"
    override fun getDurationSeconds(configuredDuration: Int): Int = CategoryRushEvaluator.DURATION_SECONDS
    override fun getTimeLimitPerWordMs(difficultyConfig: DifficultyConfig): Long = CategoryRushEvaluator.DURATION_SECONDS * 1000L
    override fun buildGrammar(words: List<Word>, currentWord: Word?): List<String> {
        val list = words.flatMap { listOf(it.word) + it.acceptedVariants }.toMutableList()
        list.add("[unk]")
        return list.distinct()
    }
}

object GameModeRegistry {
    fun fromKey(key: String): GameMode = when (key.lowercase(Locale.US)) {
        "tongue_twister", "tonguetwister" -> TongueTwisterGameMode()
        "category_rush", "categoryrush" -> CategoryRushGameMode()
        else -> VoiceRunnerGameMode()
    }
}

/**
 * Chấm điểm và phân tích câu Tongue Twister theo spec A4:
 * - Highlight từng từ theo partial result của Vosk
 * - Tỷ lệ từ đúng (accuracy)
 * - Độ trôi chảy (fluency: phạt khi ngắt quá 1.5 giây giữa các từ)
 * - Thời gian hoàn thành
 * - Thang sao 1 - 3
 */
class TongueTwisterEvaluator {

    companion object {
        const val MAX_ALLOWED_PAUSE_MS = 1500L // Ngắt quá 1.5s bị trừ điểm trôi chảy
    }

    data class EvaluationResult(
        val accuracyPercent: Float,
        val fluencyPercent: Float,
        val totalScore: Int,
        val stars: Int,
        val matchedWordIndices: Set<Int>,
        val feedback: String
    )

    /**
     * Tách câu thành danh sách từ chuẩn hóa (chữ thường, bỏ dấu câu).
     */
    fun tokenizeSentence(sentence: String): List<String> {
        return sentence
            .trim()
            .split("\\s+".toRegex())
            .map { it.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "") }
            .filter { it.isNotBlank() }
    }

    /**
     * Tìm vị trí các từ trong câu đã được người dùng đọc khớp (phục vụ highlight UI).
     */
    fun calculateMatchedIndices(targetTokens: List<String>, spokenTokens: List<String>): Set<Int> {
        val matched = mutableSetOf<Int>()
        var spokenIdx = 0

        for (targetIdx in targetTokens.indices) {
            val target = targetTokens[targetIdx]
            // Tìm từ khớp tiếp theo trong luồng đã nhận
            while (spokenIdx < spokenTokens.size) {
                val spoken = spokenTokens[spokenIdx]
                spokenIdx++
                if (spoken == target || target.contains(spoken) || spoken.contains(target)) {
                    matched.add(targetIdx)
                    break
                }
            }
        }
        return matched
    }

    /**
     * Đánh giá toàn diện lượt đọc câu Tongue Twister.
     */
    fun evaluate(
        targetSentence: String,
        spokenText: String,
        pausesBetweenWordsMs: List<Long> = emptyList(),
        completionDurationSec: Float = 0f
    ): EvaluationResult {
        val targetTokens = tokenizeSentence(targetSentence)
        val spokenTokens = tokenizeSentence(spokenText)

        if (targetTokens.isEmpty()) {
            return EvaluationResult(0f, 0f, 0, 1, emptySet(), "Câu mẫu rỗng")
        }

        val matchedIndices = calculateMatchedIndices(targetTokens, spokenTokens)
        val correctCount = matchedIndices.size
        val accuracyPercent = (correctCount.toFloat() / targetTokens.size.toFloat()).coerceIn(0f, 1f)

        // Tính điểm độ trôi chảy (Fluency)
        var longPausesCount = 0
        for (pause in pausesBetweenWordsMs) {
            if (pause > MAX_ALLOWED_PAUSE_MS) {
                longPausesCount++
            }
        }
        val fluencyPercent = (1.0f - (longPausesCount * 0.15f)).coerceIn(0f, 1f)

        // Điểm số tổng hợp thang 100: 70% độ chính xác + 30% độ trôi chảy
        val rawScore = (accuracyPercent * 70f) + (fluencyPercent * 30f)
        val totalScore = rawScore.toInt().coerceIn(0, 100)

        val stars = when {
            totalScore >= 85 -> 3
            totalScore >= 65 -> 2
            else -> 1
        }

        val feedback = when (stars) {
            3 -> "Xuất sắc! Đọc trôi chảy và rất chuẩn xác!"
            2 -> "Tốt! Chú ý giữ nhịp đọc liền mạch hơn nhé."
            else -> "Cần cố gắng thêm! Đọc chậm lại để rõ từng âm."
        }

        return EvaluationResult(
            accuracyPercent = accuracyPercent,
            fluencyPercent = fluencyPercent,
            totalScore = totalScore,
            stars = stars,
            matchedWordIndices = matchedIndices,
            feedback = feedback
        )
    }
}

/**
 * Quản lý và chống trùng lặp từ trong Category Rush theo spec A4:
 * - 30 giây thời lượng đếm ngược
 * - Đọc càng nhiều từ thuộc chủ đề càng tốt
 * - KHÔNG tính từ lặp (chuẩn hóa trước khi so sánh)
 */
class CategoryRushEvaluator(
    allowedSubjectWords: List<String>
) {
    companion object {
        const val DURATION_SECONDS = 30
    }

    private val normalizedAllowedSet: Set<String> = allowedSubjectWords
        .map { normalize(it) }
        .filter { it.isNotBlank() }
        .toSet()

    // Tập hợp lưu các từ duy nhất đã được đọc đúng
    private val _uniqueWordsSpoken = mutableSetOf<String>()
    val uniqueWordsSpoken: Set<String> get() = _uniqueWordsSpoken.toSet()

    sealed interface SubmissionResult {
        data class Accepted(val word: String, val totalUniqueCount: Int) : SubmissionResult
        data class Duplicate(val word: String) : SubmissionResult
        data class NotInSubject(val word: String) : SubmissionResult
    }

    fun submitWord(spokenWord: String): SubmissionResult {
        val norm = normalize(spokenWord)
        if (norm.isBlank()) return SubmissionResult.NotInSubject(spokenWord)

        if (!normalizedAllowedSet.contains(norm)) {
            return SubmissionResult.NotInSubject(spokenWord)
        }

        if (_uniqueWordsSpoken.contains(norm)) {
            return SubmissionResult.Duplicate(spokenWord)
        }

        _uniqueWordsSpoken.add(norm)
        return SubmissionResult.Accepted(spokenWord, _uniqueWordsSpoken.size)
    }

    fun getScore(): Int = _uniqueWordsSpoken.size * 10

    fun getStars(): Int = when {
        _uniqueWordsSpoken.size >= 12 -> 3
        _uniqueWordsSpoken.size >= 6 -> 2
        else -> 1
    }

    fun reset() {
        _uniqueWordsSpoken.clear()
    }

    private fun normalize(word: String): String {
        return word.trim().lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "")
    }
}
