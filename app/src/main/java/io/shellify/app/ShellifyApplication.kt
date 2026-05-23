package io.shellify.app

import android.app.Application
import io.shellify.app.core.adblock.AdBlocker
import io.shellify.app.core.backup.BackupManager
import io.shellify.app.core.backup.BackupSettings
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.GeckoNativeLoader
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.navigation.WebViewIntentFactory
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.data.local.AppDatabase
import io.shellify.app.data.repository.CategoryRepositoryImpl
import io.shellify.app.data.repository.NotificationRepositoryImpl
import io.shellify.app.data.repository.WebAppRepositoryImpl
import io.shellify.app.domain.usecase.CountNotificationsTodayUseCase
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteAllCategoriesUseCase
import io.shellify.app.domain.usecase.DeleteCategoryUseCase
import io.shellify.app.domain.usecase.DeleteOldNotificationsUseCase
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.FindAppsForUrlUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetCategoryByIdUseCase
import io.shellify.app.domain.usecase.GetNotificationsUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.GetWebAppByNameUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.IsDndActiveUseCase
import io.shellify.app.domain.usecase.SaveCategoryUseCase
import io.shellify.app.domain.usecase.SaveNotificationUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import io.shellify.app.presentation.linkdispatcher.LinkDispatcherServiceProvider
import io.shellify.app.presentation.shortcut.ShortcutActivity
import io.shellify.app.presentation.webview.PwaNotificationDispatcher
import io.shellify.app.presentation.webview.WebViewIntentFactoryImpl
import io.shellify.app.presentation.webview.WebViewServiceProvider
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShellifyApplication : Application(), WebViewServiceProvider, LinkDispatcherServiceProvider {
    val pendingDeepLink = MutableSharedFlow<Pair<String, String>>(replay = 1)

    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Crypto first — everything else depends on it
    val cryptoManager by lazy { CryptoManager(this) }

    private val database by lazy { AppDatabase.getInstance(this, cryptoManager) }

    val webAppRepository by lazy { WebAppRepositoryImpl(database.webAppDao()) }
    val categoryRepository by lazy { CategoryRepositoryImpl(database.categoryDao()) }

    val getWebApps by lazy { GetWebAppsUseCase(webAppRepository) }
    override val saveWebApp by lazy { SaveWebAppUseCase(webAppRepository) }
    val deleteWebApp by lazy { DeleteWebAppUseCase(webAppRepository) }
    val deleteAllApps by lazy { DeleteAllAppsUseCase(webAppRepository) }
    override val getWebAppById by lazy { GetWebAppByIdUseCase(webAppRepository) }
    val getWebAppByName by lazy { GetWebAppByNameUseCase(webAppRepository) }
    override val findAppsForUrl by lazy { FindAppsForUrlUseCase(webAppRepository) }
    override val webViewIntentFactory: WebViewIntentFactory by lazy { WebViewIntentFactoryImpl() }
    val getCategories by lazy { GetCategoriesUseCase(categoryRepository) }
    override val getCategoryById by lazy { GetCategoryByIdUseCase(categoryRepository) }
    val saveCategory by lazy { SaveCategoryUseCase(categoryRepository) }
    val deleteCategory by lazy { DeleteCategoryUseCase(categoryRepository) }
    val deleteAllCategories by lazy { DeleteAllCategoriesUseCase(categoryRepository) }

    override val themeManager by lazy { ThemeManager(this) }
    override val passwordManager by lazy { PasswordManager(this) }
    val backupSettings by lazy { BackupSettings(this, cryptoManager) }
    val backupManager by lazy {
        BackupManager(
            this,
            database,
            isolationManager.cookieJarManager,
            themeManager,
            passwordManager,
            backupSettings,
            simpleIconsManager
        )
    }

    override val geckoEngineManager by lazy { GeckoEngineManager(this) }
    val pwaAnalyzer by lazy { PwaAnalyzer.create() }
    val faviconFetcher by lazy { FaviconFetcher(this, themeManager) }
    override val adBlocker by lazy { AdBlocker() }
    val simpleIconsManager by lazy { SimpleIconsManager(this) }
    override val isolationManager by lazy { IsolationManager(this, cryptoManager, geckoEngineManager) }

    val notificationRepository by lazy { NotificationRepositoryImpl(database.notificationDao()) }
    override val saveNotification by lazy { SaveNotificationUseCase(notificationRepository) }
    override val getNotifications by lazy { GetNotificationsUseCase(notificationRepository) }
    override val deleteOldNotifications by lazy { DeleteOldNotificationsUseCase(notificationRepository) }
    override val countNotificationsToday by lazy { CountNotificationsTodayUseCase(notificationRepository) }
    val globalNotificationsEnabled: StateFlow<Boolean> by lazy {
        themeManager.globalNotificationsEnabled.stateIn(applicationScope, SharingStarted.Eagerly, false)
    }
    override val notificationDispatcher by lazy {
        PwaNotificationDispatcher(
            context = this,
            isGlobalNotificationsEnabled = { NotificationManagerCompat.from(this).areNotificationsEnabled() },
            isDndActive = IsDndActiveUseCase(),
            saveNotification = saveNotification,
            countToday = countNotificationsToday,
            getCategoryById = getCategoryById,
        )
    }

    // Tracks which WebViewActivity appIds are currently in the foreground.
    // BackgroundNotificationService consults this before creating its own GeckoSession
    // to avoid duplicate sessions for the same app (T-06-30).
    override val activeWebViewApps = MutableStateFlow<Set<Long>>(emptySet())

    override fun registerActiveApp(appId: Long) { activeWebViewApps.update { it + appId } }

    override fun unregisterActiveApp(appId: Long) { activeWebViewApps.update { it - appId } }

    override fun onCreate() {
        super.onCreate()
        PwaShortcutManager.init(ShortcutActivity::class.java)
        // Preload all GeckoView .so files for every process (main + tab/gpu children).
        // Classloader injection alone is insufficient: libxul.so's transitive dep on
        // liblgpllibs.so is resolved by the native linker namespace, not the Java classloader.
        // Pre-loading each .so via System.load() puts them in the global linker table so
        // dlopen("liblgpllibs.so") from within libxul.so succeeds in all processes.
        injectAndLoadGeckoView()
        // Prune notifications older than 30 days on every startup.
        // runCatching swallows any DB-init race since AppDatabase is by lazy.
        CoroutineScope(Dispatchers.IO).launch { runCatching { deleteOldNotifications() } }
    }

    override fun injectAndLoadGeckoView() {
        GeckoNativeLoader.injectAndLoad(this)
    }
}
