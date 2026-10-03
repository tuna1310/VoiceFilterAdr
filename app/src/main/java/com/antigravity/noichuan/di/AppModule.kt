package com.antigravity.noichuan.di

import androidx.room.Room
import com.antigravity.noichuan.data.local.AppDatabase
import com.antigravity.noichuan.data.remote.RemoteContentDataSource
import com.antigravity.noichuan.data.remote.SupabaseRemoteContentDataSource
import com.antigravity.noichuan.data.repository.ContentRepositoryImpl
import com.antigravity.noichuan.data.repository.DataStoreBookmarkRepository
import com.antigravity.noichuan.data.repository.DataStoreProgressRepository
import com.antigravity.noichuan.data.repository.FakePurchaseRepository
import com.antigravity.noichuan.data.repository.VideoRepositoryImpl
import com.antigravity.noichuan.data.service.AndroidSpeechRecognizerService
import com.antigravity.noichuan.data.service.AndroidTtsService
import com.antigravity.noichuan.data.service.GoogleSpeechRecognizerEngine
import com.antigravity.noichuan.data.service.ScoreCalculatorImpl
import com.antigravity.noichuan.data.service.VoskSpeechRecognizerEngine
import com.antigravity.noichuan.domain.repository.BookmarkRepository
import com.antigravity.noichuan.domain.repository.ProgressRepository
import com.antigravity.noichuan.domain.repository.PurchaseRepository
import com.antigravity.noichuan.domain.repository.VideoRepository
import com.antigravity.noichuan.domain.repository.WordRepository
import com.antigravity.noichuan.domain.service.ScoreCalculator
import com.antigravity.noichuan.domain.service.SpeechRecognizerEngine
import com.antigravity.noichuan.domain.service.SpeechRecognizerService
import com.antigravity.noichuan.domain.service.TtsService
import com.antigravity.noichuan.ui.screens.challenge.ChallengeViewModel
import com.antigravity.noichuan.ui.screens.home.HomeViewModel
import com.antigravity.noichuan.ui.screens.profile.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    // Local Room Database & Dao
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "noichuan_database"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().recordedVideoDao() }
    single { get<AppDatabase>().wordDao() }
    single { get<AppDatabase>().tongueTwisterDao() }
    single { get<AppDatabase>().contentVersionDao() }

    single<RemoteContentDataSource> { SupabaseRemoteContentDataSource() }

    // Repositories
    single<WordRepository> { ContentRepositoryImpl(androidContext(), get(), get()) }
    single<PurchaseRepository> { FakePurchaseRepository() }
    single<ProgressRepository> { DataStoreProgressRepository(androidContext()) }
    single<BookmarkRepository> { DataStoreBookmarkRepository(androidContext()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<com.antigravity.noichuan.domain.repository.SettingsRepository> { com.antigravity.noichuan.data.repository.DataStoreSettingsRepository(androidContext()) }

    // Services
    single<ScoreCalculator> { ScoreCalculatorImpl() }
    single<TtsService> { AndroidTtsService(androidContext()) }
    factory<SpeechRecognizerService> { AndroidSpeechRecognizerService(androidContext()) }
    single { com.antigravity.noichuan.data.service.SoundEffectManager(androidContext()) }

    // Speech Recognizer Engines
    // Vosk là engine mặc định (xử lý on-device, nhận PCM từ PcmAudioRecorder)
    single<SpeechRecognizerEngine> { VoskSpeechRecognizerEngine(androidContext()) }
    // Google SpeechRecognizer engine là dự phòng có tên riêng (tự mở mic riêng)
    single<SpeechRecognizerEngine>(named("google_engine")) { GoogleSpeechRecognizerEngine(androidContext()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { com.antigravity.noichuan.ui.screens.settings.SettingsViewModel(get(), get()) }
    viewModel { (subject: String, level: String, mode: String, wordId: String?) ->
        ChallengeViewModel(
            subjectKey = subject,
            levelKey = level,
            modeKey = mode,
            requestedWordId = wordId,
            wordRepository = get(),
            speechEngine = get(),
            ttsService = get(),
            scoreCalculator = get(),
            progressRepository = get(),
            soundEffectManager = get()
        )
    }
}
