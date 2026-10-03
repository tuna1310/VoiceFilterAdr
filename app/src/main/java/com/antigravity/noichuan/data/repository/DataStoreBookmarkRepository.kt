package com.antigravity.noichuan.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.antigravity.noichuan.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreBookmarkRepository(
    private val context: Context
) : BookmarkRepository {

    private val KEY_BOOKMARKS = stringSetPreferencesKey("bookmarked_subjects")

    override fun getBookmarkedSubjectKeys(): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_BOOKMARKS] ?: emptySet()
        }
    }

    override suspend fun toggleBookmark(subjectKey: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_BOOKMARKS] ?: emptySet()
            if (current.contains(subjectKey)) {
                preferences[KEY_BOOKMARKS] = current - subjectKey
            } else {
                preferences[KEY_BOOKMARKS] = current + subjectKey
            }
        }
    }

    override suspend fun isBookmarked(subjectKey: String): Boolean {
        val current = context.dataStore.data.map { it[KEY_BOOKMARKS] ?: emptySet() }.first()
        return current.contains(subjectKey)
    }
}
