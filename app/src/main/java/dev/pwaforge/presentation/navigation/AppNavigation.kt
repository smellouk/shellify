package dev.pwaforge.presentation.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import dev.pwaforge.PWAForgeApplication
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

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigateToTab(Screen.Home.route) },
                        icon = { Icon(Icons.Default.PhoneAndroid, null) },
                        label = { Text("Apps") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Categories.route,
                        onClick = { navController.navigateToTab(Screen.Categories.route) },
                        icon = { Icon(Icons.Default.Category, null) },
                        label = { Text("Categories") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Shortcuts.route,
                        onClick = { navController.navigateToTab(Screen.Shortcuts.route) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Shortcut, null) },
                        label = { Text("Shortcuts") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.GlobalSettings.route,
                        onClick = { navController.navigateToTab(Screen.GlobalSettings.route) },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") },
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
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = HomeViewModel(app.getWebApps, app.deleteWebApp, app.getCategories, app.saveWebApp, app.isolationManager, app),
                    onAddApp = { navController.navigate(Screen.Add.createRoute()) },
                    onEditApp = { id -> navController.navigate(Screen.Add.createRoute(id)) },
                    onOpenApp = { },
                    onOpenSettings = { id -> navController.navigate(Screen.Settings.createRoute(id)) },
                )
            }

            composable(
                route = Screen.Add.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType; defaultValue = 0L }),
            ) { back ->
                val appId = back.arguments?.getLong("appId") ?: 0L
                AddScreen(
                    viewModel = AddViewModel(appId, app.webAppRepository, app.saveWebApp,
                        app.getCategories, app.pwaAnalyzer, app.faviconFetcher),
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Settings.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType }),
            ) { back ->
                val appId = back.arguments!!.getLong("appId")
                AppSettingsScreen(
                    viewModel = AppSettingsViewModel(appId, app.webAppRepository, app.saveWebApp,
                        app.deleteWebApp, app.isolationManager, app),
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                    onOpenTranslate = { navController.navigate(Screen.TranslateConfig.createRoute(appId)) },
                )
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    viewModel = CategoryViewModel(app.getCategories, app.saveCategory, app.categoryRepository),
                )
            }

            composable(
                route = Screen.TranslateConfig.route,
                arguments = listOf(navArgument("appId") { type = NavType.LongType }),
            ) { back ->
                val appId = back.arguments!!.getLong("appId")
                TranslateConfigScreen(
                    viewModel = TranslateConfigViewModel(appId, app.webAppRepository, app.saveWebApp),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Shortcuts.route) {
                ShortcutsScreen(
                    viewModel = ShortcutsViewModel(context = app, repo = app.webAppRepository),
                )
            }

            composable(Screen.GlobalSettings.route) {
                GlobalSettingsScreen(
                    viewModel = GlobalSettingsViewModel(app.themeManager, app.isolationManager, app.webAppRepository),
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
