package com.antigravity.noichuan.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.domain.repository.BookmarkRepository
import com.antigravity.noichuan.domain.repository.ProgressRepository
import com.antigravity.noichuan.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedVideoItem(
    val id: String,
    val subjectKey: String,
    val title: String,
    val learnersCount: Int,
    val level: DifficultyLevel,
    val durationText: String,
    val targetWord: String,
    val wordId: String? = null,
    val gameMode: String = "voice_runner",
    val banner: String? = null,
    val isSample: Boolean = true
)

data class HomeUiState(
    val selectedSubjectKey: String = Subject.COLORS.key,
    val selectedLevel: DifficultyLevel = DifficultyLevel.BEGINNER,
    val selectedSubjectForMode: Subject? = null,
    val isPremium: Boolean = false,
    val showUnlockSuccessDialog: Boolean = false
)

class HomeViewModel(
    private val progressRepository: ProgressRepository,
    private val purchaseRepository: PurchaseRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val bookmarkedKeys: StateFlow<Set<String>> = bookmarkRepository.getBookmarkedSubjectKeys()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        viewModelScope.launch {
            purchaseRepository.isPremium.collect { premium ->
                _uiState.value = _uiState.value.copy(isPremium = premium)
            }
        }
    }

    fun selectSubject(subjectKey: String) {
        _uiState.value = _uiState.value.copy(selectedSubjectKey = subjectKey)
    }

    fun selectLevel(level: DifficultyLevel) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
    }

    fun openModeSelection(subject: Subject) {
        _uiState.value = _uiState.value.copy(selectedSubjectForMode = subject)
    }

    fun closeModeSelection() {
        _uiState.value = _uiState.value.copy(selectedSubjectForMode = null)
    }

    fun toggleBookmark(subjectKey: String) {
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(subjectKey)
        }
    }

    fun getFeedItemsForSubject(subjectKey: String): List<FeedVideoItem> {
        val subject = Subject.fromKey(subjectKey)
        return when (subject) {
            Subject.COLORS -> listOf(
                FeedVideoItem("col_01", subject.key, "Brown", 19740, DifficultyLevel.BEGINNER, "00:02", "Brown", "colors_b_008", "voice_runner", "file:///android_asset/banners/banner_colors_brown.webp"),
                FeedVideoItem("col_02", subject.key, "Turquoise", 33224, DifficultyLevel.EXPERT, "00:02", "Turquoise", "colors_e_031", "voice_runner", "file:///android_asset/banners/banner_colors_turquoise.webp"),
                FeedVideoItem("col_03", subject.key, "Green", 15516, DifficultyLevel.BEGINNER, "00:01", "Green", "colors_b_003", "voice_runner"),
                FeedVideoItem("col_04", subject.key, "Chartreuse", 24110, DifficultyLevel.EXPERT, "00:01", "Chartreuse", "colors_e_040", "voice_runner")
            )
            Subject.CAR_BRANDS -> listOf(
                FeedVideoItem("car_01", subject.key, "Ford", 15516, DifficultyLevel.BEGINNER, "00:01", "Ford", "car_brands_b_046", "voice_runner", "file:///android_asset/banners/banner_car_brands_ford.webp"),
                FeedVideoItem("car_02", subject.key, "Nissan", 8023, DifficultyLevel.BEGINNER, "00:01", "Nissan", "car_brands_b_052", "voice_runner", "file:///android_asset/banners/banner_car_brands_nissan.webp"),
                FeedVideoItem("car_03", subject.key, "Honda", 12450, DifficultyLevel.BEGINNER, "00:02", "Honda", "car_brands_b_047", "voice_runner"),
                FeedVideoItem("car_04", subject.key, "Ferrari", 9340, DifficultyLevel.INTERMEDIATE, "00:01", "Ferrari", "car_brands_i_061", "voice_runner")
            )
            Subject.WEEKDAYS -> listOf(
                FeedVideoItem("wk_01", subject.key, "Thursday", 24110, DifficultyLevel.BEGINNER, "00:01", "Thursday", "weekdays_b_091", "category_rush"),
                FeedVideoItem("wk_02", subject.key, "Wednesday", 18230, DifficultyLevel.BEGINNER, "00:02", "Wednesday", "weekdays_b_090", "category_rush"),
                FeedVideoItem("wk_03", subject.key, "Fortnight", 14200, DifficultyLevel.INTERMEDIATE, "00:01", "Fortnight", "weekdays_i_106", "category_rush")
            )
            Subject.ANIMALS -> listOf(
                FeedVideoItem("an_01", subject.key, "Lion", 33224, DifficultyLevel.BEGINNER, "00:02", "Lion", "animals_b_185", "voice_runner", "file:///android_asset/banners/banner_animals_lion.webp"),
                FeedVideoItem("an_02", subject.key, "Tiger", 21890, DifficultyLevel.BEGINNER, "00:02", "Tiger", "animals_b_186", "voice_runner", "file:///android_asset/banners/banner_animals_tiger.webp"),
                FeedVideoItem("an_03", subject.key, "Elephant", 18020, DifficultyLevel.BEGINNER, "00:02", "Elephant", "animals_b_195", "voice_runner")
            )
            Subject.COUNTRIES -> listOf(
                FeedVideoItem("co_01", subject.key, "America", 32261, DifficultyLevel.INTERMEDIATE, "00:01", "America", "countries_i_331", "category_rush"),
                FeedVideoItem("co_02", subject.key, "Vietnam", 19800, DifficultyLevel.BEGINNER, "00:02", "Vietnam", "countries_b_322", "category_rush")
            )
            else -> listOf(
                FeedVideoItem("${subject.key}_01", subject.key, subject.title, subject.learnersCount, DifficultyLevel.BEGINNER, "00:01", subject.title, null, subject.defaultGameMode, subject.banner)
            )
        }
    }
}
