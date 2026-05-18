# Android Instrumented Tests

All instrumented tests for the entire app live here — never inside feature or core modules.

Run: `./gradlew connectedDebugAndroidTest`

---

## Directory layout

```
androidTest/
  e2e/        smoke and integration tests against live app components (real WebViews, real URLs)
  mock/
    db/       Room DAO tests (in-memory database)
    navigation/  NavHost route tests
    screen/   Composable screen tests (mocked ViewModels)
  util/
    FakeData.kt   centralised domain-object builders — use these, never construct manually
```

---

## Key gotchas

### `internal` is invisible here

`androidTest` is a separate Kotlin module. `internal` declarations from `src/main/` cannot be imported. For test hooks, use `@VisibleForTesting` with `public` visibility in the production companion object, and always reset to `null` in `@After`.

### Transitive `implementation` deps are not on the compile classpath

If a type lives in a module that is `implementation(project(":core:foo"))` inside a feature, it is NOT importable here. Add it explicitly:

```kotlin
// app/build.gradle.kts
androidTestImplementation(project(":core:foo"))
```

---

## Patterns

### Composable screen (mocked ViewModel)

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun appList_displaysAppName() {
        val vm = mockk<HomeViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(
            HomeUiState(apps = listOf(FakeData.webApp(name = "Reddit")), hasAnyApps = true)
        )

        composeTestRule.setContent {
            ShellifyTheme {
                HomeScreen(viewModel = vm, onAddApp = {}, onOpenApp = {})
            }
        }

        composeTestRule.onNodeWithText("Reddit").assertIsDisplayed()
    }
}
```

### Room DAO

```kotlin
@RunWith(AndroidJUnit4::class)
class WebAppDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WebAppDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.webAppDao()
    }

    @After fun tearDown() = db.close()

    @Test
    fun insertAndRetrieve() = runTest {
        dao.insert(FakeData.webAppEntity(id = 1L, name = "Reddit"))
        assertEquals("Reddit", dao.getById(1L)?.name)
    }
}
```

### WebViewActivity E2E (real WebView, real URLs)

Use `EXTRA_PREVIEW_URL` to skip the DB lookup. Navigate via the `@VisibleForTesting navigateTo()` helper. Use `PageLoadIdlingResource` (backed by `WebViewActivity.pageFinishedCallback`) to wait for page loads without `Thread.sleep`. Use a `CountDownLatch` on `onDestroy()` for cleanup — do **not** call `scenario.close()` as it calls `waitForIdleSync()` which hangs while Compose has pending frame callbacks.

```kotlin
@RunWith(AndroidJUnit4::class)
class WebViewBackNavigationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val pageLoad = PageLoadIdlingResource()

    @Before fun setUp() {
        IdlingRegistry.getInstance().register(pageLoad)
        WebViewActivity.pageFinishedCallback = { pageLoad.onPageFinished() }
    }

    @After fun tearDown() {
        IdlingRegistry.getInstance().unregister(pageLoad)
        WebViewActivity.pageFinishedCallback = null
    }

    @Test
    fun backFromTerms_navigatesInHistory_doesNotExitApp() {
        val scenario = ActivityScenario.launch<WebViewActivity>(homeIntent())

        pageLoad.awaitIdle()   // home page loaded
        pageLoad.reset()

        scenario.onActivity { it.navigateTo("https://shellify.app/terms.html") }
        pageLoad.awaitIdle()   // terms page loaded

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        assertNotEquals(Lifecycle.State.DESTROYED, scenario.state)

        val destroyed = CountDownLatch(1)
        scenario.onActivity { activity ->
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) = destroyed.countDown()
            })
            activity.finish()
        }
        destroyed.await(10, TimeUnit.SECONDS)
    }

    private fun homeIntent() = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.EXTRA_PREVIEW_URL, "https://shellify.app")
        .putExtra(WebViewActivity.EXTRA_PREVIEW_NAME, "Shellify")
}
```

**Key hooks on `WebViewActivity` (both `@VisibleForTesting`):**
- `pageFinishedCallback` — companion `var` invoked from `onPageFinished()`; wire to `PageLoadIdlingResource.onPageFinished()` in `@Before`
- `navigateTo(url)` — instance method delegating to `engine.loadUrl()`

---

## Available dependencies

| Catalog key | Purpose |
|---|---|
| `libs.mockk.android` | MockK for instrumented tests |
| `libs.androidx.test.ext.junit` | `@RunWith(AndroidJUnit4::class)`, `ActivityScenario` |
| `libs.androidx.test.runner` | `AndroidJUnitRunner`, `InstrumentationRegistry` |
| `libs.androidx.test.rules` | `ActivityTestRule` etc. |
| `libs.espresso.core` | UI interaction helpers |
| `libs.compose.ui.test.junit4` | `createComposeRule()` |
| `libs.androidx.room.testing` | `MigrationTestHelper`, in-memory DB |
| `libs.kotlinx.coroutines.test` | `runTest` |
