package com.antigravity.noichuan.data.repository

import android.content.Context
import com.antigravity.noichuan.data.model.Word
import com.antigravity.noichuan.domain.repository.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AssetWordRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : WordRepository {

    private val mutex = Mutex()
    private var cachedWords: List<Word>? = null

    override suspend fun getAllWords(): List<Word> = withContext(Dispatchers.IO) {
        cachedWords?.let { return@withContext it }
        mutex.withLock {
            cachedWords?.let { return@withLock it }
            val words = loadWordsFromAssets()
            cachedWords = words
            words
        }
    }

    override suspend fun getWordsBySubjectAndLevel(subject: String, level: String): List<Word> {
        return getAllWords().filter {
            it.subject.equals(subject, ignoreCase = true) &&
                    it.level.equals(level, ignoreCase = true)
        }
    }

    override suspend fun getWordById(id: String): Word? {
        return getAllWords().firstOrNull { it.id == id }
    }

    private fun loadWordsFromAssets(): List<Word> {
        return try {
            val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<Word>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
