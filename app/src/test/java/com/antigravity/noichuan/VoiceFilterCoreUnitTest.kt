package com.antigravity.noichuan

import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Word
import com.antigravity.noichuan.data.service.VoskSpeechRecognizerEngine
import com.antigravity.noichuan.domain.game.CategoryRushEvaluator
import com.antigravity.noichuan.domain.game.TongueTwisterEvaluator
import com.antigravity.noichuan.domain.game.TongueTwisterGameMode
import com.antigravity.noichuan.domain.game.VoiceRunnerGameMode
import com.antigravity.noichuan.domain.service.PronunciationMatcher
import com.antigravity.noichuan.domain.service.SilenceWatchdog
import com.antigravity.noichuan.domain.service.VoskGrammarBuilder
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VoiceFilterCoreUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun voskGrammarBuilder_appendsUnknownToken_andFormatsValidJsonArray() {
        val json = VoskGrammarBuilder.buildGrammarJson(listOf("Red", "Blue", "Red"))
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"[unk]\""))
        assertTrue(json.contains("red"))
        assertTrue(json.contains("blue"))
    }

    @Test
    fun voskGrammarBuilder_handlesEmptyInputSafely() {
        val json = VoskGrammarBuilder.buildGrammarJson(emptyList())
        assertEquals("[\"[unk]\"]", json)
    }

    @Test
    fun voskSpeechEngine_parseHelpers_andModelVersionCheckWork() {
        assertEquals("brown", VoskSpeechRecognizerEngine.parseResultText("""{"text":"  brown  "}"""))
        assertEquals("", VoskSpeechRecognizerEngine.parseResultText("invalid-json"))
        assertEquals("yellow", VoskSpeechRecognizerEngine.parsePartialText("""{"partial" : "yellow"}"""))
        assertEquals("", VoskSpeechRecognizerEngine.parsePartialText("{}"))

        val modelDir = tempFolder.newFolder("vosk-model-small-en-us")
        assertFalse(VoskSpeechRecognizerEngine.isModelVersionValid(modelDir))

        File(modelDir, "conf").mkdirs()
        File(modelDir, "conf/model.conf").writeText("test")
        File(modelDir, VoskSpeechRecognizerEngine.VERSION_FILE_NAME)
            .writeText(VoskSpeechRecognizerEngine.MODEL_VERSION)
        assertTrue(VoskSpeechRecognizerEngine.isModelVersionValid(modelDir))
    }

    @Test
    fun pronunciationMatcher_enforcesDifficultyAndVariantsAndMinimalPairs() {
        val matcher = PronunciationMatcher()

        // Exact & Noise
        assertTrue(matcher.check("Red", "red", DifficultyLevel.EXPERT).matched)
        assertFalse(matcher.check("Red", "[unk]", DifficultyLevel.BEGINNER).matched)

        // Accepted variants & Homophones & Contractions
        assertTrue(matcher.check("Red", "read", DifficultyLevel.EXPERT, listOf("read")).matched)
        assertTrue(matcher.check("Night", "knight", DifficultyLevel.EXPERT).matched)
        assertTrue(matcher.check("Can't", "cannot", DifficultyLevel.EXPERT).matched)

        // Minimal pair rejected on Expert but tolerated as near-phonetic on Beginner
        assertFalse(matcher.check("Ship", "sheep", DifficultyLevel.EXPERT).matched)
        assertTrue(matcher.check("Ship", "sheep", DifficultyLevel.BEGINNER).matched)
    }

    @Test
    fun gameModes_buildGrammarAndEvaluateCoreRules() {
        val runner = VoiceRunnerGameMode()
        val word = Word(
            id = "c1",
            word = "Turquoise",
            phonetic = "/ˈtɜːr.kɔɪz/",
            tip = "Test tip",
            level = "expert",
            subject = "colors",
            acceptedVariants = listOf("turkoise")
        )
        assertEquals(listOf("Turquoise", "turkoise"), runner.buildGrammar(listOf(word), word))

        val twisterMode = TongueTwisterGameMode()
        val ttWord = Word(
            id = "tt1",
            word = "Red lorry yellow lorry",
            phonetic = "/rɛd/",
            tip = "Tip",
            level = "basic",
            subject = "tongue_twister",
            gameMode = "tongue_twister"
        )
        assertEquals(listOf("red", "lorry", "yellow", "[unk]"), twisterMode.buildGrammar(listOf(ttWord), ttWord))
        val eval = TongueTwisterEvaluator()
            .evaluate("Red lorry yellow lorry", "red lorry yellow", emptyList())
        assertTrue(eval.accuracyPercent >= 0.7f)
        assertTrue(eval.totalScore >= 70)

        val rush = CategoryRushEvaluator(listOf("Lion", "Tiger", "Bear"))
        assertTrue(rush.submitWord("lion") is CategoryRushEvaluator.SubmissionResult.Accepted)
        assertTrue(rush.submitWord("LION") is CategoryRushEvaluator.SubmissionResult.Duplicate)
        assertTrue(rush.submitWord("Banana") is CategoryRushEvaluator.SubmissionResult.NotInSubject)
        assertEquals(10, rush.getScore())
    }

    @Test
    fun silenceWatchdog_triggersTimeoutOnlyAfterQuietWindow() {
        var timedOut = false
        val watchdog = SilenceWatchdog(
            silenceTimeoutMs = 4000L,
            minSpeechRmsThreshold = 200f,
            onTimeout = { timedOut = true }
        )
        watchdog.start(currentTimeMs = 1000L)

        val loudChunk = ShortArray(160) { 1000 }
        val quietChunk = ShortArray(160) { 10 }

        watchdog.processPcmChunk(quietChunk, quietChunk.size, currentTimeMs = 3000L)
        assertFalse(timedOut)

        watchdog.processPcmChunk(loudChunk, loudChunk.size, currentTimeMs = 4500L)
        assertFalse(timedOut)

        watchdog.processPcmChunk(quietChunk, quietChunk.size, currentTimeMs = 8600L)
        assertTrue(timedOut)
    }

    @Test
    fun wordsAsset_containsAllRequiredTopicsModesAndEnglishTips() {
        val candidates = listOf(
            File("src/main/assets/words.json"),
            File("app/src/main/assets/words.json")
        )
        val assetFile = candidates.first { it.exists() }
        val json = Json { ignoreUnknownKeys = true }
        val words = json.decodeFromString<List<Word>>(assetFile.readText())

        assertEquals(600, words.size)
        val subjects = words.map { it.subject }.toSet()
        assertTrue(subjects.contains("daily_words"))
        assertTrue(subjects.contains("travel_food"))
        assertTrue(subjects.contains("entertainment_sports"))
        assertTrue(subjects.contains("slang_brainrot"))
        assertTrue(subjects.contains("tongue_twister"))

        val modes = words.mapNotNull { it.gameMode }.toSet()
        assertTrue(modes.contains("voice_runner"))
        assertTrue(modes.contains("category_rush"))
        assertTrue(modes.contains("tongue_twister"))

        val nonAsciiTip = words.firstOrNull { word -> word.tip.any { ch -> ch.code > 127 } }
        assertEquals(null, nonAsciiTip)
    }
}
