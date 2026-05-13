package dev.pwaforge.presentation.navigation

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pwaforge.R
import dev.pwaforge.core.locale.LocaleHelper
import dev.pwaforge.presentation.onboarding.OnboardingScreen
import dev.pwaforge.presentation.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import dev.pwaforge.PWAForgeApplication
import dev.pwaforge.core.engine.GeckoInstallState
import dev.pwaforge.presentation.add.AddScreen
import dev.pwaforge.presentation.add.AddViewModel
import dev.pwaforge.presentation.category.CategoryScreen
import dev.pwaforge.presentation.category.CategoryViewModel
import dev.pwaforge.presentation.home.HomeScreen
import dev.pwaforge.presentation.home.HomeViewModel
import dev.pwaforge.presentation.settings.AppSettingsScreen
import dev.pwaforge.presentation.settings.AppSettingsViewModel
import dev.pwaforge.presentation.settings.GlobalSettingsScreen
import dev.pwaforge.presentation.settings.GlobalSettingsViewModel
import dev.pwaforge.presentation.shortcuts.ShortcutsScreen
import dev.pwaforge.presentation.shortcuts.ShortcutsViewModel
import dev.pwaforge.presentation.translate.TranslateConfigScreen
import dev.pwaforge.presentation.translate.TranslateConfigViewModel
import dev.pwaforge.presentation.theme.Dimens

private val topLevelRoutes = setOf(
    Screen.Home.route,
    Screen.Categories.route,
    Screen.Shortcuts.route,
    Screen.GlobalSettings.route,
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: PWAForgeApplication,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val geckoInstallState by app.geckoEngineManager.installState.collectAsState()
    val geckoInstalled = geckoInstallState is GeckoInstallState.Installed
            || geckoInstallState is GeckoInstallState.Downloading
            || geckoInstallState is GeckoInstallState.Installing
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguageCode(context) }
    val coroutineScope = rememberCoroutineScope()

    // null = not yet loaded, false = show onboarding, true = show home
    val onboardingDone by app.themeManager.onboardingDone.collectAsState(initial = null)
    if (onboardingDone == null) return

    val startDestination = if (onboardingDone == true) Screen.Home.route else Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceSm),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            data class NavItem(val route: String, val icon: ImageVector, val label: String)
                            listOf(
                                NavItem(Screen.Home.route, Icons.Default.PhoneAndroid, stringResource(R.string.nav_apps)),
                                NavItem(Screen.Categories.route, Icons.Default.Layers, stringResource(R.string.nav_categories)),
                                NavItem(Screen.Shortcuts.route, Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.nav_shortcuts)),
                                NavItem(Screen.GlobalSettings.route, Icons.Default.Settings, stringResource(R.string.nav_settings)),
                            ).forEach { item ->
                                val active = currentRoute == item.route
                                Column(
                                    modifier = Modifier.weight(1f).clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { navController.navigateToTab(item.route) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = Dimens.sizeIllustrationTile, height = Dimens.size4xl)
                                            .background(
                                                if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                RoundedCornerShape(Dimens.cornerXl),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            item.icon,
                                            null,
                                            modifier = Modifier.size(Dimens.sizeLg),
                                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
            popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = remember { HomeViewModel(app.getWebApps, app.deleteWebApp, app.getCategories, app.saveWebApp, app.isolationManager, app, app.pwaAnalyzer, app.faviconFetcher) },
                    geckoInstalled = geckoInstalled,
                    currentLanguage = currentLanguage,
                    onLanguageChange = { code ->
                        coroutineScope.launch { app.themeManager.setLanguageCode(code) }
                        LocaleHelper.setLanguageCode(context, code)
                        (context as? Activity)?.recreate()
                    },
                    onAddApp = { navController.navigate(Screen.Add.createRoute()) },
                    onEditApp = { id -> navController.navigate(Screen.Add.createRoute(id)) },
                    onOpenApp = { },
                    onOpenSettings = { id -> navController.navigate(Screen.Settings.createRoute(id)) },
                )
            }

            composable(
                route = Screen.Add.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType; defaultValue = 0L }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) { back ->
                val appId = back.arguments?.getLong("appId") ?: 0L
                AddScreen(
                    viewModel = remember(appId) { AddViewModel(appId, app.webAppRepository, app.saveWebApp,
                        app.getCategories, app.pwaAnalyzer, app.faviconFetcher,
                        app.geckoEngineManager, app.themeManager,
                        app.simpleIconsManager, app) },
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Settings.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) { back ->
                val appId = back.arguments!!.getLong("appId")
                AppSettingsScreen(
                    viewModel = remember(appId) { AppSettingsViewModel(appId, app.webAppRepository, app.saveWebApp,
                        app.deleteWebApp, app.isolationManager, app,
                        app.pwaAnalyzer, app.faviconFetcher, app.simpleIconsManager) },
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                )
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    viewModel = remember { CategoryViewModel(app.getCategories, app.saveCategory, app.categoryRepository) },
                )
            }

            composable(Screen.Shortcuts.route) {
                ShortcutsScreen(
                    viewModel = remember { ShortcutsViewModel(context = app, repo = app.webAppRepository, analyzer = app.pwaAnalyzer, faviconFetcher = app.faviconFetcher) },
                )
            }

            composable(Screen.GlobalSettings.route) {
                GlobalSettingsScreen(
                    viewModel = remember {
                        GlobalSettingsViewModel(
                            app.themeManager, app.isolationManager, app.webAppRepository,
                            app.categoryRepository, app.passwordManager,
                            app.backupSettings, app.backupManager, app,
                            app.geckoEngineManager, app.simpleIconsManager,
                        )
                    },
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    viewModel = remember {
                        OnboardingViewModel(
                            themeManager = app.themeManager,
                            passwordManager = app.passwordManager,
                            backupSettings = app.backupSettings,
                            saveWebApp = app.saveWebApp,
                            pwaAnalyzer = app.pwaAnalyzer,
                            faviconFetcher = app.faviconFetcher,
                            onFinished = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            },
                        )
                    },
                    onFinished = {},
                    onLanguageChange = { code ->
                        coroutineScope.launch { app.themeManager.setLanguageCode(code) }
                        LocaleHelper.setLanguageCode(context, code)
                        (context as? Activity)?.recreate()
                    },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
