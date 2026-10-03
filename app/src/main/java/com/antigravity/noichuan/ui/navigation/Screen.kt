package com.antigravity.noichuan.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object Subjects : Screen("subjects")
    data object Profile : Screen("profile")
    data object Premium : Screen("premium")

    data object SubjectDetail : Screen("subject_detail/{subjectKey}") {
        fun createRoute(subjectKey: String): String {
            return "subject_detail/${Uri.encode(subjectKey)}"
        }
    }

    data object Challenge : Screen("challenge/{subject}/{level}/{wordId}/{gameMode}") {
        fun createRoute(
            subject: String,
            level: String,
            wordId: String? = null,
            gameMode: String = "voice_runner"
        ): String {
            return "challenge/${Uri.encode(subject)}/${Uri.encode(level)}/${Uri.encode(wordId ?: "_")}/${Uri.encode(gameMode)}"
        }
    }

    data object Result : Screen("result/{subjectKey}/{levelKey}/{durationSec}/{wordsCompleted}?videoPath={videoPath}&score={score}&stars={stars}&combo={combo}") {
        fun createRoute(
            subjectKey: String,
            levelKey: String,
            durationSec: Int,
            wordsCompleted: Int,
            videoPath: String? = null,
            score: Int = 0,
            stars: Int = 1,
            combo: Int = 0
        ): String {
            val encodedVideo = if (!videoPath.isNullOrBlank()) Uri.encode(videoPath) else ""
            return "result/${Uri.encode(subjectKey)}/${Uri.encode(levelKey)}/$durationSec/$wordsCompleted?videoPath=$encodedVideo&score=$score&stars=$stars&combo=$combo"
        }
    }

    data object Settings : Screen("settings")
}
