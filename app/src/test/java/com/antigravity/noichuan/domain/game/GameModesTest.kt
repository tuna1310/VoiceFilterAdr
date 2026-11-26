package com.antigravity.noichuan.domain.game

import com.antigravity.noichuan.data.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModesTest {

    private fun word(id: String, text: String) = Word(
        id = id,
        word = text,
        phonetic = "",
        tip = "Tip",
        level = "basic",
        subject = "colors",
        gameMode = "voice_runner"
    )

    @Test
    fun `game state machine transitions from countdown to playing and advances on match`() {
        val sm = GameStateMachine()
        val deck = listOf(word("1", "Red"), word("2", "Blue"))
        sm.processAction(GameAction.Start(words = deck, durationSeconds = 15, timeLimitPerWordMs = 3000L))
        assertTrue(sm.state.value is GameState.Countdown)

        sm.processAction(GameAction.CountdownFinish)
        assertTrue(sm.state.value is GameState.Playing)

        sm.processAction(GameAction.WordMatched(spokenWord = "Red", pointsAwarded = 10))
        // Advance past the 300ms gate transition animation
        sm.processAction(GameAction.Tick(deltaMs = 350L))
        val afterMatch = sm.state.value as GameState.Playing
        assertEquals(1, afterMatch.currentWordIndex)
        assertEquals(1, afterMatch.wordsMatchedCount)
        assertEquals(10, afterMatch.score)
    }

    @Test
    fun `game state machine decrements lives on word timeout and finishes after 3 misses`() {
        val sm = GameStateMachine()
        val deck = listOf(word("1", "Red"), word("2", "Blue"), word("3", "Green"), word("4", "Pink"))
        sm.processAction(GameAction.Start(words = deck, durationSeconds = 15, timeLimitPerWordMs = 3000L))
        sm.processAction(GameAction.CountdownFinish)

        sm.processAction(GameAction.WordTimeout)
        sm.processAction(GameAction.Tick(deltaMs = 350L))
        assertEquals(2, (sm.state.value as GameState.Playing).livesRemaining)

        sm.processAction(GameAction.WordTimeout)
        sm.processAction(GameAction.Tick(deltaMs = 350L))
        assertEquals(1, (sm.state.value as GameState.Playing).livesRemaining)

        sm.processAction(GameAction.WordTimeout)
        assertTrue(sm.state.value is GameState.Finished)
        val finished = sm.state.value as GameState.Finished
        assertEquals(GameOverReason.OUT_OF_LIVES, finished.reason)
    }

    @Test
    fun `tongue twister evaluator scores fluency penalties cleanly`() {
        val evaluator = TongueTwisterEvaluator()
        val fluent = evaluator.evaluate("Red lorry yellow lorry", "red lorry yellow lorry", emptyList())
        val paused = evaluator.evaluate("Red lorry yellow lorry", "red lorry yellow lorry", listOf(2000L, 2500L))
        assertEquals(100, fluent.totalScore)
        assertEquals(3, fluent.stars)
        assertTrue(paused.totalScore < fluent.totalScore)
    }
}
