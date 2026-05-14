package io.shellify.app.presentation.navigation

import android.app.Activity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import io.shellify.app.ShellifyApplication
import io.shellify.app.core.engine.GeckoInstallState
import io.shellify.app.core.locale.LocaleHelper
import io.shellify.app.presentation.add.AddScreen
import io.shellify.app.presentation.add.AddViewModel
import io.shellify.app.presentation.category.CategoryScreen
import io.shellify.app.presentation.category.CategoryViewModel
import io.shellify.app.presentation.home.HomeScreen
import io.shellify.app.presentation.home.HomeViewModel
import io.shellify.app.presentation.onboarding.OnboardingScreen
import io.shellify.app.presentation.onboarding.OnboardingViewModel
import io.shellify.app.presentation.settings.AppSettingsScreen
import io.shellify.app.presentation.settings.AppSettingsViewModel
import io.shellify.app.presentation.settings.GlobalSettingsScreen
import io.shellify.app.presentation.settings.GlobalSettingsViewModel
import io.shellify.app.presentation.settings.ChangelogScreen
import io.shellify.app.presentation.settings.LicensesScreen
import io.shellify.app.presentation.shortcuts.ShortcutsScreen
import io.shellify.app.presentation.shortcuts.ShortcutsViewModel
import io.shellify.app.presentation.theme.Dimens
import io.shellify.core.ui.R
import kotlinx.coroutines.launch

private const val NAV_ANIM_DURATION = 200

private val topLevelRoutes = setOf(
    Screen.Home.route,
    Screen.Categories.route,
    Screen.Shortcuts.route,
    Screen.GlobalSettings.route,
)

@Suppress("CognitiveComplexMethod")
@Composable
fun AppNavigation(
    navController: NavHostController,
    app: ShellifyApplication,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val geckoInstallState by app.geckoEngineManager.installState.collectAsState()
    val geckoInstalled = geckoInstallState is GeckoInstallState.Installed ||
        geckoInstallState is GeckoInstallState.Downloading ||
        geckoInstallState is GeckoInstallState.Installing
    val context = LocalContext.current
    val currentLanguage = remember { LocaleHelper.getLanguageCode(context) }
    val coroutineScope = rememberCoroutineScope()

    val onboardingDone by app.themeManager.onboardingDone.collectAsState(initial = null)
    if (onboardingDone == null) return

    val startDestination =
        if (onboardingDone == true) Screen.Home.route else Screen.Onboarding.route

    LaunchedEffect(Unit) {
        app.pendingDeepLink.collect { (url, name) ->
            navController.navigate(Screen.Add.createRoute(url = url, name = name))
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.spaceXs, vertical = Dimens.spaceSm),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            data class NavItem(
                                val route: String,
                                val icon: ImageVector,
                                val label: String
                            )
                            listOf(
                                NavItem(Screen.Home.route, Icons.Default.PhoneAndroid, stringResource(R.string.nav_apps)),
                                NavItem(Screen.Categories.route, Icons.Default.Layers, stringResource(R.string.nav_categories)),
                                NavItem(Screen.Shortcuts.route, Icons.AutoMirrored.Filled.Shortcut, stringResource(R.string.nav_shortcuts)),
                                NavItem(Screen.GlobalSettings.route, Icons.Default.Settings, stringResource(R.string.nav_settings)),
                            ).forEach { item ->
                                val active = currentRoute == item.route
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { navController.navigateToTab(item.route) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = Dimens.sizeIllustrationTile,
                                                height = Dimens.size4xl
                                            )
                                            .background(
                                                if (active) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent,
                                                RoundedCornerShape(Dimens.cornerXl),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            item.icon,
                                            null,
                                            modifier = Modifier.size(Dimens.sizeLg),
                                            tint = if (active) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
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
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            popExitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = remember {
                        HomeViewModel(
                            app.getWebApps,
                            app.deleteWebApp,
                            app.getCategories,
                            app.saveWebApp,
                            app.isolationManager,
                            app,
                            app.pwaAnalyzer,
                            app.faviconFetcher
                        )
                    },
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
                arguments = listOf(
                    navArgument("appId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) { back ->
                val appId = back.arguments?.getLong("appId") ?: 0L
                val prefilledUrl = back.arguments?.getString("url") ?: ""
                val prefilledName = back.arguments?.getString("name") ?: ""
                AddScreen(
                    viewModel = remember(appId) {
                        AddViewModel(
                            appId, app.getWebAppById, app.getWebAppByName, app.saveWebApp,
                            app.getCategories, app.pwaAnalyzer, app.faviconFetcher,
                            app.geckoEngineManager, app.themeManager,
                            app.simpleIconsManager, app,
                            prefilledUrl = prefilledUrl, prefilledName = prefilledName
                        )
                    },
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
                    viewModel = remember(appId) {
                        AppSettingsViewModel(
                            appId,
                            app.getWebAppById,
                            app.saveWebApp,
                            app.deleteWebApp,
                            app.isolationManager,
                            app,
                            app.pwaAnalyzer,
                            app.faviconFetcher,
                            app.simpleIconsManager,
                            app.passwordManager,
                            app.geckoEngineManager
                        )
                    },
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    },
                )
            }

            composable(Screen.Categories.route) {
                CategoryScreen(
                    viewModel = remember {
                        CategoryViewModel(
                            app.getCategories,
                            app.saveCategory,
                            app.deleteCategory,
                        )
                    },
                )
            }

            composable(Screen.Shortcuts.route) {
                ShortcutsScreen(
                    viewModel = remember {
                        ShortcutsViewModel(
                            context = app,
                            getWebApps = app.getWebApps,
                            saveWebApp = app.saveWebApp,
                            analyzer = app.pwaAnalyzer,
                            faviconFetcher = app.faviconFetcher,
                        )
                    },
                )
            }

            composable(Screen.GlobalSettings.route) {
                GlobalSettingsScreen(
                    viewModel = remember {
                        GlobalSettingsViewModel(
                            app.themeManager, app.isolationManager,
                            app.getWebApps, app.saveWebApp,
                            app.deleteAllApps, app.deleteAllCategories,
                            app.passwordManager, app.backupSettings, app.backupManager, app,
                            app.geckoEngineManager, app.simpleIconsManager,
                            onGeckoInstalled = { app.injectAndLoadGeckoView() },
                        )
                    },
                    onChangelog = { navController.navigate(Screen.Changelog.route) },
                    onLicenses = { navController.navigate(Screen.Licenses.route) },
                )
            }

            composable(
                route = Screen.Changelog.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) {
                ChangelogScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.Licenses.route,
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
            ) {
                LicensesScreen(onBack = { navController.popBackStack() })
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
