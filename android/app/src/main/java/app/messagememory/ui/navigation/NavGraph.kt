package app.messagememory.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.messagememory.di.AppContainer
import app.messagememory.permissions.NotificationAccessState
import app.messagememory.ui.conversation.ConversationScreen
import app.messagememory.ui.dashboard.DashboardScreen
import app.messagememory.ui.media.MediaViewerScreen
import app.messagememory.ui.onboarding.OnboardingScreen
import app.messagememory.ui.search.SearchScreen
import app.messagememory.ui.settings.DiagnosticsScreen
import app.messagememory.ui.settings.SamsungGuidanceScreen
import app.messagememory.ui.settings.SettingsScreen
import app.messagememory.ui.settings.StorageManagementScreen

@Composable
fun MessageMemoryNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (NotificationAccessState.isEnabled(context)) Destinations.DASHBOARD else Destinations.ONBOARDING

    ReevaluateOnboardingOnResume(navController)

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onOpenNotificationAccessSettings = {
                    context.startActivity(NotificationAccessState.settingsIntent())
                },
            )
        }
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                container = container,
                onOpenConversation = { id -> navController.navigate(Destinations.conversation(id)) },
                onOpenSearch = { navController.navigate(Destinations.SEARCH) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                onOpenNotificationAccessSettings = { context.startActivity(NotificationAccessState.settingsIntent()) },
            )
        }
        composable(Destinations.CONVERSATION) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("conversationId")?.toLongOrNull() ?: return@composable
            ConversationScreen(
                container = container,
                conversationId = id,
                onBack = { navController.popBackStack() },
                onOpenMedia = { messageId -> navController.navigate(Destinations.mediaViewer(messageId)) },
            )
        }
        composable(Destinations.MEDIA_VIEWER) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("messageId")?.toLongOrNull() ?: return@composable
            MediaViewerScreen(container = container, messageId = id, onBack = { navController.popBackStack() })
        }
        composable(Destinations.SEARCH) {
            SearchScreen(container = container, onBack = { navController.popBackStack() })
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenStorage = { navController.navigate(Destinations.STORAGE_MANAGEMENT) },
                onOpenDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) },
                onOpenSamsungGuidance = { navController.navigate(Destinations.SAMSUNG_GUIDANCE) },
                onOpenNotificationAccessSettings = { context.startActivity(NotificationAccessState.settingsIntent()) },
            )
        }
        composable(Destinations.STORAGE_MANAGEMENT) {
            StorageManagementScreen(container = container, onBack = { navController.popBackStack() })
        }
        composable(Destinations.DIAGNOSTICS) {
            DiagnosticsScreen(container = container, onBack = { navController.popBackStack() })
        }
        composable(Destinations.SAMSUNG_GUIDANCE) {
            SamsungGuidanceScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Moves off Onboarding the moment Notification Access is granted (e.g. returning from system Settings). */
@Composable
private fun ReevaluateOnboardingOnResume(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == Destinations.ONBOARDING && NotificationAccessState.isEnabled(context)) {
                    navController.navigate(Destinations.DASHBOARD) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
