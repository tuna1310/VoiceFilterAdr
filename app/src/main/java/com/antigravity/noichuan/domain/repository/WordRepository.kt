package com.antigravity.noichuan.domain.repository

import com.antigravity.noichuan.data.model.Word

interface WordRepository {
    suspend fun getAllWords(): List<Word>
    suspend fun getWordsBySubjectAndLevel(subject: String, level: String): List<Word>
    suspend fun getWordById(id: String): Word?
}
