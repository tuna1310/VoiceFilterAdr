package com.antigravity.noichuan.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.noichuan.data.local.RecordedVideoEntity
import com.antigravity.noichuan.domain.repository.BookmarkRepository
import com.antigravity.noichuan.domain.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val videoRepository: VideoRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    val recordedVideos: StateFlow<List<RecordedVideoEntity>> = videoRepository.getAllVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarkedKeys: StateFlow<Set<String>> = bookmarkRepository.getBookmarkedSubjectKeys()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun deleteVideo(video: RecordedVideoEntity) {
        viewModelScope.launch {
            videoRepository.deleteVideo(video)
        }
    }

    fun toggleBookmark(subjectKey: String) {
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(subjectKey)
        }
    }
}
