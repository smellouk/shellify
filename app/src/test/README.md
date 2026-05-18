# Unit Tests (JVM)

JVM tests that run without a device. The patterns here apply to **all** modules:
- `app/src/test/` — screenshot regression and architecture tests
- `feature/*/src/test/` — ViewModel tests
- `core/*/src/test/` — use case, mapper, and utility tests

Run: `./gradlew testDebugUnitTest`

---

## Directory layout (app/src/test only)

```
test/
  snapshots/          Roborazzi golden images — commit alongside screen changes
  architecture/       Konsist architecture rule tests
  screenshot/         Robolectric + Roborazzi screenshot tests per screen
```

Feature and core modules only contain plain JUnit tests — no snapshots directory.

---

## Patterns

### ViewModel

```kotlin
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads apps on init`() = runTest {
        val getApps = mockk<GetWebAppsUseCase>()
        coEvery { getApps() } returns flowOf(listOf(FakeData.webApp()))

        val vm = HomeViewModel(getApps)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.apps.size)
    }
}
```

Notes:
- Always set `Dispatchers.setMain(UnconfinedTestDispatcher())` and reset in `@After`
- Use `coEvery` / `coVerify` for suspend functions, `every` / `verify` for regular ones
- `advanceUntilIdle()` drains all pending coroutines before asserting

### Use case / mapper

```kotlin
class SaveWebAppUseCaseTest {

    private val repo = mockk<WebAppRepository>()
    private val useCase = SaveWebAppUseCase(repo)

    @Test
    fun `delegates to repository`() = runTest {
        val app = FakeData.webApp()
        coEvery { repo.save(app) } just Runs

        useCase(app)

        coVerify(exactly = 1) { repo.save(app) }
    }
}
```

### Screenshot (Robolectric + Roborazzi) — app/src/test/ only

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun emptyState() {
        val vm = mockk<HomeViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(HomeUiState())

        composeTestRule.setContent { ShellifyTheme { HomeScreen(viewModel = vm) } }

        composeTestRule.onRoot().captureRoboImage()
    }
}
```

After any UI change, regenerate goldens and **commit the updated images**:

```bash
./gradlew recordRoborazziDebug
```

### Architecture rules (Konsist) — app/src/test/ only

Architecture tests run automatically as part of `testDebugUnitTest`. Do not suppress failures — fix the violation instead.

---

## Required dependencies per module type

**Feature / core module** (`src/test/`):
```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test) // if testing coroutines
```

**App module** (`src/test/`) — already configured, adding screenshot tests needs:
```kotlin
testImplementation(libs.robolectric)
testImplementation(libs.roborazzi)
testImplementation(libs.roborazzi.compose)
testImplementation(platform(libs.compose.bom))
testImplementation(libs.compose.ui.test.junit4)
```

---

## `internal` visibility

Unlike `androidTest`, the `src/test/` source set is compiled together with `src/main/` as the same Kotlin module. `internal` declarations are fully accessible here — no `@VisibleForTesting` needed for unit tests.
