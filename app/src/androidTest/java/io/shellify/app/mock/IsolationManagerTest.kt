package io.shellify.app.mock

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import io.mockk.mockk
import io.mockk.verify
import io.shellify.app.core.crypto.CryptoManager
import io.shellify.app.core.engine.GeckoEngineManager
import io.shellify.app.core.isolation.IsolationManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val ISOLATION_ID = "test-isolation-clear-data"
private const val TEST_COOKIE_URL = "https://example.shellify.test"
private const val TEST_COOKIE = "session=xyz789"

@RunWith(AndroidJUnit4::class)
class IsolationManagerTest {

    private lateinit var context: Context
    private lateinit var manager: IsolationManager
    private val geckoEngineManager: GeckoEngineManager = mockk(relaxed = true)

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        manager = IsolationManager(
            context = context,
            crypto = CryptoManager(context),
            geckoEngineManager = geckoEngineManager,
        )
        manager.cookieJarManager.deleteFor(ISOLATION_ID)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    @After
    fun tearDown() = runTest {
        manager.cookieJarManager.deleteFor(ISOLATION_ID)
    }

    @Test
    fun clearData_doesNotThrow() = runTest {
        manager.clearData(ISOLATION_ID)
    }

    @Test
    fun clearData_notifiesGeckoEngineManager() = runTest {
        manager.clearData(ISOLATION_ID)
        verify { geckoEngineManager.clearDataForContext(ISOLATION_ID) }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.S_V2) // CookieJarManager is only used on API < 33; WebView Profiles handle isolation above
    fun clearData_removesStoredCookieJar() = runTest {

        // Store a cookie jar for this isolation ID, then clear it.
        val cm = CookieManager.getInstance()
        cm.setCookie(TEST_COOKIE_URL, TEST_COOKIE)
        manager.cookieJarManager.saveAndClearFor(ISOLATION_ID, setOf(TEST_COOKIE_URL))

        manager.clearData(ISOLATION_ID)

        // Restoring after clearData should yield no cookies for this isolation ID.
        manager.cookieJarManager.restoreFor(ISOLATION_ID)
        val cookies = cm.getCookie(TEST_COOKIE_URL)
        assertTrue("Expected no cookies after clearData but found: $cookies", cookies.isNullOrBlank())
    }
}
