package io.shellify.app

import android.app.Application
import android.os.Build
import io.shellify.app.core.adblock.AdBlocker
import io.shellify.app.core.engine.GeckoNativeLoader
import io.shellify.app.core.shortcut.PwaShortcutManager
import io.shellify.app.presentation.shortcut.ShortcutActivity
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.backup.BackupManager
import io.shellify.app.core.backup.BackupSettings
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.core.iconpack.SimpleIconsManager
import io.shellify.app.core.pwa.FaviconFetcher
import io.shellify.app.core.pwa.PwaAnalyzer
import io.shellify.app.data.local.AppDatabase
import io.shellify.app.data.repository.CategoryRepositoryImpl
import io.shellify.app.data.repository.WebAppRepositoryImpl
import io.shellify.app.domain.usecase.DeleteWebAppUseCase
import io.shellify.app.domain.usecase.GetCategoriesUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.SaveCategoryUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.MutableSharedFlow

class ShellifyApplication : Application() {
    val pendingDeepLink = MutableSharedFlow<Pair<String, String>>(replay = 1)
    // Crypto first — everything else depends on it
    val cryptoManager by lazy { CryptoManager(this) }

    private val database by lazy { AppDatabase.getInstance(this, cryptoManager) }

    val webAppRepository by lazy { WebAppRepositoryImpl(database.webAppDao()) }
    val categoryRepository by lazy { CategoryRepositoryImpl(database.categoryDao()) }

    val getWebApps by lazy { GetWebAppsUseCase(webAppRepository) }
    val saveWebApp by lazy { SaveWebAppUseCase(webAppRepository) }
    val deleteWebApp by lazy { DeleteWebAppUseCase(webAppRepository) }
    val getCategories by lazy { GetCategoriesUseCase(categoryRepository) }
    val saveCategory by lazy { SaveCategoryUseCase(categoryRepository) }

    val themeManager by lazy { ThemeManager(this) }
    val passwordManager by lazy { PasswordManager(this) }
    val backupSettings by lazy { BackupSettings(this, cryptoManager) }
    val backupManager by lazy {
        BackupManager(this, database, isolationManager.cookieJarManager, themeManager, passwordManager, backupSettings, simpleIconsManager)
    }

    val geckoEngineManager by lazy { GeckoEngineManager(this) }
    val pwaAnalyzer by lazy { PwaAnalyzer.create() }
    val faviconFetcher by lazy { FaviconFetcher(this, themeManager) }
    val adBlocker by lazy { AdBlocker() }
    val simpleIconsManager by lazy { SimpleIconsManager(this) }
    val isolationManager by lazy { IsolationManager(this, cryptoManager, geckoEngineManager) }

    override fun onCreate() {
        super.onCreate()
        PwaShortcutManager.init(ShortcutActivity::class.java)
        // Preload all GeckoView .so files for every process (main + tab/gpu children).
        // Classloader injection alone is insufficient: libxul.so's transitive dep on
        // liblgpllibs.so is resolved by the native linker namespace, not the Java classloader.
        // Pre-loading each .so via System.load() puts them in the global linker table so
        // dlopen("liblgpllibs.so") from within libxul.so succeeds in all processes.
        injectAndLoadGeckoView()
    }

    fun injectAndLoadGeckoView() {
        GeckoNativeLoader.injectAndLoad(this)
    }
}
