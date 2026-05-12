package dev.pwaforge.presentation.navigation

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.pwaforge.R
import dev.pwaforge.core.locale.LocaleHelper
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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


    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigateToTab(Screen.Home.route) },
                        icon = { Icon(Icons.Default.PhoneAndroid, null) },
                        label = { Text(stringResource(R.string.nav_apps)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Categories.route,
                        onClick = { navController.navigateToTab(Screen.Categories.route) },
                        icon = { Icon(Icons.Default.Category, null) },
                        label = { Text(stringResource(R.string.nav_categories)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Shortcuts.route,
                        onClick = { navController.navigateToTab(Screen.Shortcuts.route) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Shortcut, null) },
                        label = { Text(stringResource(R.string.nav_shortcuts)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.GlobalSettings.route,
                        onClick = { navController.navigateToTab(Screen.GlobalSettings.route) },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
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
                    viewModel = remember { HomeViewModel(app.getWebApps, app.deleteWebApp, app.getCategories, app.saveWebApp, app.isolationManager, app) },
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
                        app.geckoEngineManager, app.themeManager) },
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
                        app.deleteWebApp, app.isolationManager, app) },
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onOpenTranslate = { navController.navigate(Screen.TranslateConfig.createRoute(appId)) },
                )
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    viewModel = remember { CategoryViewModel(app.getCategories, app.saveCategory, app.categoryRepository) },
                )
            }

            composable(
                route = Screen.TranslateConfig.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) { back ->
                val appId = back.arguments!!.getLong("appId")
                TranslateConfigScreen(
                    viewModel = remember(appId) { TranslateConfigViewModel(appId, app.webAppRepository, app.saveWebApp) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Shortcuts.route) {
                ShortcutsScreen(
                    viewModel = remember { ShortcutsViewModel(context = app, repo = app.webAppRepository) },
                )
            }

            composable(Screen.GlobalSettings.route) {
                GlobalSettingsScreen(
                    viewModel = remember {
                        GlobalSettingsViewModel(
                            app.themeManager, app.isolationManager, app.webAppRepository,
                            app.categoryRepository, app.passwordManager,
                            app.backupSettings, app.backupManager, app,
                            app.geckoEngineManager,
                        )
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
