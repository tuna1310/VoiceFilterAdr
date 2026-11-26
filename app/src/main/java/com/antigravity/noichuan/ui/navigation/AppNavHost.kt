package com.antigravity.noichuan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.ui.screens.challenge.ChallengeScreen
import com.antigravity.noichuan.ui.screens.challenge.ChallengeViewModel
import com.antigravity.noichuan.ui.screens.detail.SubjectDetailScreen
import com.antigravity.noichuan.ui.screens.home.HomeScreen
import com.antigravity.noichuan.ui.screens.home.HomeViewModel
import com.antigravity.noichuan.ui.screens.onboarding.OnboardingScreen
import com.antigravity.noichuan.ui.screens.permission.PermissionScreen
import com.antigravity.noichuan.ui.screens.premium.PremiumScreen
import com.antigravity.noichuan.ui.screens.profile.ProfileScreen
import com.antigravity.noichuan.ui.screens.profile.ProfileViewModel
import com.antigravity.noichuan.ui.screens.result.ResultScreen
import com.antigravity.noichuan.ui.screens.settings.SettingsScreen
import com.antigravity.noichuan.ui.screens.settings.SettingsViewModel
import com.antigravity.noichuan.ui.screens.splash.SplashScreen
import com.antigravity.noichuan.ui.screens.subjects.SubjectsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Permission Screen
        composable(Screen.Permission.route) {
            PermissionScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Home Screen
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = koinViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSubjectDetail = { subjectKey ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectKey))
                },
                onNavigateToRecording = { subjectKey, levelKey, wordId, gameMode ->
                    navController.navigate(Screen.Challenge.createRoute(subjectKey, levelKey, wordId, gameMode))
                },
                onNavigateToSubjects = {
                    navController.navigate(Screen.Subjects.route)
                },
                onNavigateToPremium = {
                    navController.navigate(Screen.Premium.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 5. Subjects Screen
        composable(Screen.Subjects.route) {
            SubjectsScreen(
                onBack = { navController.popBackStack() },
                onSelectSubject = { subjectKey, levelKey ->
                    navController.navigate(Screen.SubjectDetail.createRoute(subjectKey))
                }
            )
        }

        // 6. Subject Detail Screen
        composable(
            route = Screen.SubjectDetail.route,
            arguments = listOf(
                navArgument("subjectKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectKey = backStackEntry.arguments?.getString("subjectKey") ?: Subject.COLORS.key
            SubjectDetailScreen(
                subjectKey = subjectKey,
                onBack = { navController.popBackStack() },
                onNavigateToRecording = { subKey, levelKey, modeKey ->
                    navController.navigate(Screen.Challenge.createRoute(subKey, levelKey, null, modeKey))
                }
            )
        }

        // 7. AR Recording / Challenge Screen
        composable(
            route = Screen.Challenge.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("level") { type = NavType.StringType },
                navArgument("wordId") { type = NavType.StringType },
                navArgument("gameMode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: Subject.COLORS.key
            val level = backStackEntry.arguments?.getString("level") ?: DifficultyLevel.BEGINNER.key
            val rawWordId = backStackEntry.arguments?.getString("wordId")
            val wordId = rawWordId?.takeUnless { it == "_" || it.isBlank() }
            val gameMode = backStackEntry.arguments?.getString("gameMode") ?: Subject.fromKey(subject).defaultGameMode

            val challengeViewModel: ChallengeViewModel = koinViewModel {
                parametersOf(subject, level, gameMode, wordId)
            }

            ChallengeScreen(
                viewModel = challengeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { videoPath, subKey, lvlKey, durationSec, wordsCompleted, score, stars, combo ->
                    navController.navigate(
                        Screen.Result.createRoute(
                            subjectKey = subKey,
                            levelKey = lvlKey,
                            durationSec = durationSec,
                            wordsCompleted = wordsCompleted,
                            videoPath = videoPath,
                            score = score,
                            stars = stars,
                            combo = combo
                        )
                    ) {
                        popUpTo(Screen.Challenge.route) { inclusive = true }
                    }
                }
            )
        }

        // 8. Post-Recording / Result Preview Screen
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("subjectKey") { type = NavType.StringType },
                navArgument("levelKey") { type = NavType.StringType },
                navArgument("durationSec") { type = NavType.IntType },
                navArgument("wordsCompleted") { type = NavType.IntType },
                navArgument("videoPath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("score") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("stars") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("combo") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val subjectKey = backStackEntry.arguments?.getString("subjectKey") ?: Subject.COLORS.key
            val levelKey = backStackEntry.arguments?.getString("levelKey") ?: DifficultyLevel.BEGINNER.key
            val durationSec = backStackEntry.arguments?.getInt("durationSec") ?: 15
            val wordsCompleted = backStackEntry.arguments?.getInt("wordsCompleted") ?: 1
            val videoPath = backStackEntry.arguments?.getString("videoPath")
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val stars = backStackEntry.arguments?.getInt("stars") ?: 1
            val combo = backStackEntry.arguments?.getInt("combo") ?: 0

            ResultScreen(
                videoPath = videoPath,
                subjectKey = subjectKey,
                levelKey = levelKey,
                durationSec = durationSec,
                wordsCompleted = wordsCompleted,
                score = score,
                stars = stars,
                combo = combo,
                onNavigateBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onRetake = {
                    navController.navigate(Screen.Challenge.createRoute(subjectKey, levelKey, null, Subject.fromKey(subjectKey).defaultGameMode)) {
                        popUpTo(Screen.Result.route) { inclusive = true }
                    }
                }
            )
        }

        // 9. Profile Screen
        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = koinViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToRecording = { subjectKey, levelKey ->
                    navController.navigate(Screen.Challenge.createRoute(subjectKey, levelKey, null, Subject.fromKey(subjectKey).defaultGameMode))
                },
                onPlayVideo = { }
            )
        }

        // 10. Premium / Paywall Screen
        composable(Screen.Premium.route) {
            PremiumScreen(
                onClose = { navController.popBackStack() }
            )
        }

        // 11. Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
