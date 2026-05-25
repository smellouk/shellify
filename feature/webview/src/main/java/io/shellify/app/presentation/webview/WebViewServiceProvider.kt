package io.shellify.app.presentation.webview

import io.shellify.app.core.adblock.AdBlocker
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.engine.TorManager
import io.shellify.app.core.isolation.IsolationManager
import io.shellify.app.core.security.PasswordManager
import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.domain.usecase.ClearNetworkLogsUseCase
import io.shellify.app.domain.usecase.CountNotificationsTodayUseCase
import io.shellify.app.domain.usecase.DeleteAllAppsUseCase
import io.shellify.app.domain.usecase.DeleteOldNetworkLogsUseCase
import io.shellify.app.domain.usecase.DeleteOldNotificationsUseCase
import io.shellify.app.domain.usecase.GetCategoryByIdUseCase
import io.shellify.app.domain.usecase.GetNetworkLogUseCase
import io.shellify.app.domain.usecase.GetNotificationsUseCase
import io.shellify.app.domain.usecase.GetWebAppByIdUseCase
import io.shellify.app.domain.usecase.GetWebAppsUseCase
import io.shellify.app.domain.usecase.LogNetworkRequestUseCase
import io.shellify.app.domain.usecase.SaveNotificationUseCase
import io.shellify.app.domain.usecase.SaveWebAppUseCase
import kotlinx.coroutines.flow.StateFlow

interface WebViewServiceProvider {
    val isolationManager: IsolationManager
    val passwordManager: PasswordManager
    val geckoEngineManager: GeckoEngineManager
    val themeManager: ThemeManager
    val adBlocker: AdBlocker
    val getWebAppById: GetWebAppByIdUseCase
    val saveWebApp: SaveWebAppUseCase
    val deleteAllApps: DeleteAllAppsUseCase
    val getWebApps: GetWebAppsUseCase
    val notificationDispatcher: PwaNotificationDispatcher?
    val getCategoryById: GetCategoryByIdUseCase
    val saveNotification: SaveNotificationUseCase
    val getNotifications: GetNotificationsUseCase
    val deleteOldNotifications: DeleteOldNotificationsUseCase
    val countNotificationsToday: CountNotificationsTodayUseCase
    val logNetworkRequest: LogNetworkRequestUseCase
    val clearNetworkLogs: ClearNetworkLogsUseCase
    val getNetworkLog: GetNetworkLogUseCase
    val deleteOldNetworkLogs: DeleteOldNetworkLogsUseCase
    val torManager: TorManager

    // Active-app tracking: called by WebViewActivity to coordinate with BackgroundNotificationService.
    val activeWebViewApps: StateFlow<Set<Long>>
    fun registerActiveApp(appId: Long)
    fun unregisterActiveApp(appId: Long)

    // Loads GeckoView native libraries — must be called before accessing geckoEngineManager.getRuntime().
    fun injectAndLoadGeckoView()
}
