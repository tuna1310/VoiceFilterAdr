package com.antigravity.noichuan.domain.service

import com.antigravity.noichuan.data.model.DifficultyLevel
import org.apache.commons.codec.language.DoubleMetaphone
import org.apache.commons.codec.language.Soundex

/**
 * Ket qua cua mot lan kiem tra phat am.
 * @param matched  true = chap nhan ung vien la dung
 * @param reason   mo ta ngan de debug
 * @param score    diem thuong de xuat (0-100)
 */
data class MatchResult(
    val matched: Boolean,
    val reason: String,
    val score: Int = if (matched) 100 else 0
)

/**
 * Kiem tra phat am theo 10 buoc.
 * Nguong Levenshtein (theo spec, chua do tren giong that):
 *   BEGINNER >= 0.60, INTERMEDIATE >= 0.75, EXPERT >= 0.85
 */
class PronunciationMatcher {

    private val doubleMetaphone = DoubleMetaphone()
    private val soundex = Soundex()

    private val thresholdByLevel = mapOf(
        DifficultyLevel.BEGINNER     to 0.60f,
        DifficultyLevel.INTERMEDIATE to 0.75f,
        DifficultyLevel.EXPERT       to 0.85f
    )

    private val minimalPairRules: Map<String, List<Pair<String, DifficultyLevel>>> = mapOf(
        "ship"  to listOf("sheep"  to DifficultyLevel.EXPERT),
        "sheep" to listOf("ship"   to DifficultyLevel.EXPERT),
        "fit"   to listOf("feet"   to DifficultyLevel.EXPERT),
        "feet"  to listOf("fit"    to DifficultyLevel.EXPERT),
        "pen"   to listOf("pan"    to DifficultyLevel.INTERMEDIATE),
        "pan"   to listOf("pen"    to DifficultyLevel.INTERMEDIATE),
        "bit"   to listOf("beat"   to DifficultyLevel.EXPERT),
        "beat"  to listOf("bit"    to DifficultyLevel.EXPERT),
        "sit"   to listOf("seat"   to DifficultyLevel.EXPERT),
        "seat"  to listOf("sit"    to DifficultyLevel.EXPERT),
        "live"  to listOf("leave"  to DifficultyLevel.EXPERT),
        "leave" to listOf("live"   to DifficultyLevel.EXPERT)
    )

    private val nearPhoneticPairs: Map<String, Set<String>> = mapOf(
        "ship"  to setOf("sheep"), "sheep" to setOf("ship"),
        "fit"   to setOf("feet"),  "feet"  to setOf("fit"),
        "bit"   to setOf("beat"),  "beat"  to setOf("bit"),
        "sit"   to setOf("seat"),  "seat"  to setOf("sit"),
        "live"  to setOf("leave"), "leave" to setOf("live")
    )

    private val homophones: Map<String, Set<String>> = mapOf(
        "write" to setOf("right"), "right" to setOf("write"),
        "sun"   to setOf("son"),   "son"   to setOf("sun"),
        "sea"   to setOf("see"),   "see"   to setOf("sea"),
        "two"   to setOf("to","too"), "to" to setOf("two","too"), "too" to setOf("two","to"),
        "their" to setOf("there","theyre"), "there" to setOf("their","theyre"),
        "theyre" to setOf("their","there"),
        "hear"  to setOf("here"),  "here"  to setOf("hear"),
        "bare"  to setOf("bear"),  "bear"  to setOf("bare"),
        "by"    to setOf("bye","buy"), "bye" to setOf("by","buy"), "buy" to setOf("by","bye"),
        "flower" to setOf("flour"), "flour" to setOf("flower"),
        "knight" to setOf("night"), "night" to setOf("knight"),
        "know"  to setOf("no"),    "no"    to setOf("know"),
        "mail"  to setOf("male"),  "male"  to setOf("mail"),
        "meet"  to setOf("meat"),  "meat"  to setOf("meet"),
        "pair"  to setOf("pear"),  "pear"  to setOf("pair"),
        "peace" to setOf("piece"), "piece" to setOf("peace"),
        "plain" to setOf("plane"), "plane" to setOf("plain"),
        "rain"  to setOf("reign","rein"), "reign" to setOf("rain","rein"), "rein" to setOf("rain","reign"),
        "road"  to setOf("rode"),  "rode"  to setOf("road"),
        "some"  to setOf("sum"),   "sum"   to setOf("some"),
        "steal" to setOf("steel"), "steel" to setOf("steal"),
        "tail"  to setOf("tale"),  "tale"  to setOf("tail"),
        "weak"  to setOf("week"),  "week"  to setOf("weak"),
        "which" to setOf("witch"), "witch" to setOf("which"),
        "whole" to setOf("hole"),  "hole"  to setOf("whole"),
        "wood"  to setOf("would"), "would" to setOf("wood")
    )

    private val contractionGroups: List<Set<String>> = listOf(
        setOf("cant","cannot","can not"), setOf("dont","do not"),
        setOf("wont","will not"), setOf("its","it is"),
        setOf("im","i am"), setOf("ive","i have"), setOf("ill","i will"),
        setOf("id","i would","i had"),
        setOf("youre","you are"), setOf("youve","you have"), setOf("youll","you will"), setOf("youd","you would"),
        setOf("hes","he is","he has"), setOf("shes","she is","she has"),
        setOf("were","we are"), setOf("weve","we have"), setOf("well","we will"),
        setOf("theyre","they are"), setOf("theyve","they have"), setOf("theyll","they will"),
        setOf("thats","that is"), setOf("theres","there is"), setOf("whats","what is"),
        setOf("lets","let us"),
        setOf("isnt","is not"), setOf("arent","are not"), setOf("wasnt","was not"), setOf("werent","were not"),
        setOf("hasnt","has not"), setOf("havent","have not"), setOf("hadnt","had not"),
        setOf("doesnt","does not"), setOf("didnt","did not"),
        setOf("wouldnt","would not"), setOf("shouldnt","should not"), setOf("couldnt","could not"),
        setOf("mustnt","must not"), setOf("neednt","need not")
    )

    private val digitToWord: Map<String, String> = mapOf(
        "0" to "zero","1" to "one","2" to "two","3" to "three","4" to "four",
        "5" to "five","6" to "six","7" to "seven","8" to "eight","9" to "nine",
        "10" to "ten","11" to "eleven","12" to "twelve","13" to "thirteen",
        "14" to "fourteen","15" to "fifteen","20" to "twenty","100" to "one hundred"
    )

    private val noiseTokens: Set<String> = setOf(
        "[unk]","unk","hmm","hm","uh","um","ah","oh","err","er",""
    )

    fun checkCandidates(
        target: String,
        candidates: List<String>,
        level: DifficultyLevel,
        acceptedVariants: List<String> = emptyList()
    ): MatchResult {
        for (candidate in candidates) {
            val result = check(target, candidate, level, acceptedVariants)
            if (result.matched) return result
        }
        return MatchResult(matched = false, reason = "no_match", score = 0)
    }

    fun check(
        target: String,
        candidate: String,
        level: DifficultyLevel,
        acceptedVariants: List<String> = emptyList()
    ): MatchResult {
        val normTarget    = normalize(target)
        val normCandidate = normalize(candidate)

        // 1. Noise: reject if blank, in noise set, or ALL tokens are noise tokens
        val isAllNoise = normCandidate.isBlank() ||
            normCandidate in noiseTokens ||
            normCandidate.split(" ").all { it in noiseTokens }
        if (isAllNoise)
            return MatchResult(matched = false, reason = "noise")

        // 2. acceptedVariants from Word model
        if (acceptedVariants.map { normalize(it) }.any { it == normCandidate })
            return MatchResult(matched = true, reason = "accepted_variant", score = 100)

        // 3. Minimal pair rejection
        val bannedPairs = minimalPairRules[normTarget]
        if (bannedPairs != null) {
            for ((bannedCandidate, bannedFromLevel) in bannedPairs) {
                if (normCandidate == bannedCandidate && isLevelAtLeast(level, bannedFromLevel))
                    return MatchResult(matched = false, reason = "minimal_pair:$bannedCandidate", score = 0)
            }
        }

        // 4. Exact
        if (normTarget == normCandidate)
            return MatchResult(matched = true, reason = "exact", score = 100)

        // 5. Contraction
        if (isSameContractionGroup(normTarget, normCandidate))
            return MatchResult(matched = true, reason = "contraction", score = 100)

        // 6. Homophone
        if (homophones[normTarget]?.contains(normCandidate) == true)
            return MatchResult(matched = true, reason = "homophone", score = 100)

        // 7. Number word
        val targetAsWord    = digitToWord[normTarget]
        val candidateAsWord = digitToWord[normCandidate]
        if ((targetAsWord != null && targetAsWord == normCandidate) ||
            (candidateAsWord != null && candidateAsWord == normTarget))
            return MatchResult(matched = true, reason = "number_word", score = 100)

        // 8. Near-phonetic pair (BEGINNER+INTERMEDIATE; EXPERT blocked by step 3)
        if (nearPhoneticPairs[normTarget]?.contains(normCandidate) == true)
            return MatchResult(matched = true, reason = "near_phonetic", score = 85)

        // 9. Double Metaphone / Soundex
        val phoneticResult = checkPhonetic(normTarget, normCandidate, level)
        if (phoneticResult != null) return phoneticResult

        // 10. Levenshtein ratio
        val ratio = levenshteinRatio(normTarget, normCandidate)
        val threshold = thresholdByLevel[level] ?: 0.75f
        return if (ratio >= threshold) {
            MatchResult(matched = true, reason = "near:${"%.2f".format(ratio)}", score = (ratio * 100).toInt().coerceIn(0, 99))
        } else {
            MatchResult(matched = false, reason = "below_threshold:${"%.2f".format(ratio)}")
        }
    }

    /**
     * Phonetic check via Double Metaphone + Soundex.
     * BEGINNER/INTERMEDIATE: phonetic match = accept (score 80).
     * EXPERT: phonetic alone insufficient; returns null so step 10 (Levenshtein) decides.
     */
    private fun checkPhonetic(normTarget: String, normCandidate: String, level: DifficultyLevel): MatchResult? {
        if (normTarget.contains(' ') || normCandidate.contains(' ')) return null

        val dm1T  = doubleMetaphone.doubleMetaphone(normTarget)
        val dm1C  = doubleMetaphone.doubleMetaphone(normCandidate)
        val dm2T  = doubleMetaphone.doubleMetaphone(normTarget, true)
        val dm2C  = doubleMetaphone.doubleMetaphone(normCandidate, true)

        val dmMatch = dm1T != null && dm1T.isNotEmpty() &&
            (dm1T == dm1C || dm1T == dm2C || dm2T == dm1C || dm2T == dm2C)

        val sxT = runCatching { soundex.encode(normTarget) }.getOrNull()
        val sxC = runCatching { soundex.encode(normCandidate) }.getOrNull()
        val sxMatch = sxT != null && sxT.isNotEmpty() && sxT == sxC

        if (!dmMatch && !sxMatch) return null

        if (level == DifficultyLevel.EXPERT) return null  // Expert: phonetic cong diem nhung khong quyet dinh; Levenshtein quyet

        if (level == DifficultyLevel.INTERMEDIATE &&
            levenshteinRatio(normTarget, normCandidate) < (thresholdByLevel[DifficultyLevel.INTERMEDIATE] ?: 0.75f)
        ) {
            return null
        }

        val reason = "phonetic:" + buildString {
            if (dmMatch) append("dm")
            if (sxMatch) append(if (dmMatch) "+sx" else "sx")
        }
        return MatchResult(matched = true, reason = reason, score = 80)
    }

    internal fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun isLevelAtLeast(level: DifficultyLevel, minLevel: DifficultyLevel): Boolean {
        val order = listOf(DifficultyLevel.BEGINNER, DifficultyLevel.INTERMEDIATE, DifficultyLevel.EXPERT)
        return order.indexOf(level) >= order.indexOf(minLevel)
    }

    private fun isSameContractionGroup(a: String, b: String): Boolean =
        contractionGroups.any { group -> a in group && b in group }

    internal fun levenshteinRatio(a: String, b: String): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val lenA = a.length; val lenB = b.length
        val dp = Array(lenA + 1) { IntArray(lenB + 1) }
        for (i in 0..lenA) dp[i][0] = i
        for (j in 0..lenB) dp[0][j] = j
        for (i in 1..lenA) for (j in 1..lenB) {
            dp[i][j] = if (a[i-1] == b[j-1]) dp[i-1][j-1]
                       else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
        }
        return 1f - dp[lenA][lenB].toFloat() / maxOf(lenA, lenB).toFloat()
    }
}
