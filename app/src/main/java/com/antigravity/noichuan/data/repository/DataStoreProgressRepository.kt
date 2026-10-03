package com.antigravity.noichuan.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antigravity.noichuan.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noichuan_progress")

class DataStoreProgressRepository(
    private val context: Context
) : ProgressRepository {

    companion object {
        val KEY_STREAK = intPreferencesKey("user_streak")
        val KEY_LAST_PLAYED_DATE = stringPreferencesKey("last_played_date")
        val KEY_TOTAL_SCORE = intPreferencesKey("total_score")
        val KEY_COMPLETED_WORDS = stringSetPreferencesKey("completed_words")
        fun keyStarsForSubject(subject: String) = intPreferencesKey("stars_$subject")

        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun computeStreakUpdate(
            currentStreak: Int,
            lastPlayedStr: String?,
            today: LocalDate = LocalDate.now()
        ): Pair<Int, String> {
            val todayStr = today.format(DATE_FORMATTER)
            if (lastPlayedStr.isNullOrBlank()) {
                return 1 to todayStr
            }

            return try {
                val lastDate = LocalDate.parse(lastPlayedStr, DATE_FORMATTER)
                val daysDiff = ChronoUnit.DAYS.between(lastDate, today)

                when {
                    daysDiff == 0L -> currentStreak.coerceAtLeast(1) to todayStr
                    daysDiff == 1L -> (currentStreak + 1) to todayStr
                    else -> 1 to todayStr // Missed more than 1 day, reset streak to 1
                }
            } catch (e: Exception) {
                1 to todayStr
            }
        }
    }

    override val streak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_STREAK] ?: 0
    }

    override val totalScore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOTAL_SCORE] ?: 0
    }

    override val completedWordIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_COMPLETED_WORDS] ?: emptySet()
    }

    override val starsBySubject: Flow<Map<String, Int>> = context.dataStore.data.map { preferences ->
        val subjects = listOf("cinema", "music", "food", "travel", "science", "life")
        subjects.associateWith { sub ->
            preferences[keyStarsForSubject(sub)] ?: 0
        }
    }

    override suspend fun checkAndUpdateStreak() {
        context.dataStore.edit { prefs ->
            val currentStreak = prefs[KEY_STREAK] ?: 0
            val lastPlayed = prefs[KEY_LAST_PLAYED_DATE]
            val today = LocalDate.now()

            val (newStreak, newDateStr) = computeStreakUpdate(currentStreak, lastPlayed, today)
            prefs[KEY_STREAK] = newStreak
            prefs[KEY_LAST_PLAYED_DATE] = newDateStr
        }
    }

    override suspend fun recordCompletion(
        wordId: String,
        subject: String,
        score: Int,
        stars: Int
    ) {
        context.dataStore.edit { prefs ->
            // Update total score
            val currentTotal = prefs[KEY_TOTAL_SCORE] ?: 0
            prefs[KEY_TOTAL_SCORE] = currentTotal + score

            // Update subject stars
            val subKey = keyStarsForSubject(subject)
            val currentStars = prefs[subKey] ?: 0
            prefs[subKey] = currentStars + stars

            // Update completed words set
            val completed = (prefs[KEY_COMPLETED_WORDS] ?: emptySet()).toMutableSet()
            completed.add(wordId)
            prefs[KEY_COMPLETED_WORDS] = completed

            // Update streak
            val currentStreak = prefs[KEY_STREAK] ?: 0
            val lastPlayed = prefs[KEY_LAST_PLAYED_DATE]
            val today = LocalDate.now()
            val (newStreak, newDateStr) = computeStreakUpdate(currentStreak, lastPlayed, today)
            prefs[KEY_STREAK] = newStreak
            prefs[KEY_LAST_PLAYED_DATE] = newDateStr
        }
    }
}
