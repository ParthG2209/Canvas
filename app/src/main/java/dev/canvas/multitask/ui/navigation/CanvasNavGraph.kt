package dev.canvas.multitask.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.canvas.multitask.data.prefs.PreferencesManager
import dev.canvas.multitask.ui.home.HomeScreen
import dev.canvas.multitask.ui.onboarding.OnboardingScreen
import dev.canvas.multitask.ui.picker.AppPickerScreen
import dev.canvas.multitask.ui.settings.SettingsScreen
import dev.canvas.multitask.ui.workspace.CanvasWorkspaceScreen

/**
 * Main navigation graph for Canvas.
 * Decides start destination based on onboarding completion state.
 */
@Composable
fun CanvasNavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val isOnboardingComplete by preferencesManager.isOnboardingComplete.collectAsState(initial = false)

    val startDestination = if (isOnboardingComplete) Routes.HOME else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 }
        }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNewSession = {
                    navController.navigate(Routes.APP_PICKER)
                },
                onSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.APP_PICKER) {
            AppPickerScreen(
                onLaunch = { _ ->
                    navController.navigate(Routes.WORKSPACE) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Routes.WORKSPACE) {
            CanvasWorkspaceScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onReOnboard = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
