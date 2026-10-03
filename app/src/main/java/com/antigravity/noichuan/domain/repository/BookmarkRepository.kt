package com.antigravity.noichuan.domain.repository

import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getBookmarkedSubjectKeys(): Flow<Set<String>>
    suspend fun toggleBookmark(subjectKey: String)
    suspend fun isBookmarked(subjectKey: String): Boolean
}
