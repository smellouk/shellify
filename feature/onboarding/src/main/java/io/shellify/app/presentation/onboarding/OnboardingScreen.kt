package io.shellify.app.presentation.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.shellify.core.ui.R
import io.shellify.app.core.backup.BackupSchedule
import io.shellify.app.core.locale.LocaleHelper
import io.shellify.app.core.theme.ThemeMode
import io.shellify.app.presentation.theme.ACCENT_COLORS
import io.shellify.app.presentation.theme.CategoryReadingBg
import io.shellify.app.presentation.theme.CategoryReadingFg
import io.shellify.app.presentation.theme.CategoryToolsFg
import io.shellify.app.presentation.theme.Dimens
import io.shellify.app.presentation.theme.SuggestionChatBg
import io.shellify.app.presentation.theme.SuggestionChatFg
import io.shellify.app.presentation.theme.SuggestionVideoBg
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PAGE_COUNT = 7

private val SUGGESTION_URLS = mapOf(
    "reddit" to "reddit.com",
    "discord" to "discord.com",
    "telegram" to "web.telegram.org",
    "github" to "github.com",
    "mastodon" to "mastodon.social",
    "proton" to "mail.proton.me",
    "bitwarden" to "vault.bitwarden.com",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onLanguageChange: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = state.page, pageCount = { PAGE_COUNT })

    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page)
            pagerState.animateScrollToPage(state.page, animationSpec = spring(stiffness = 400f))
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        viewModel.goTo(pagerState.currentPage - 1)
    }

    var secPassword by remember { mutableStateOf("") }
    var secConfirm by remember { mutableStateOf("") }
    val secValid = secPassword.length >= 6 && secPassword == secConfirm

    val suggestionNames = mapOf(
        "reddit" to stringResource(R.string.onboarding_quickpicks_reddit),
        "discord" to stringResource(R.string.onboarding_quickpicks_discord),
        "telegram" to stringResource(R.string.onboarding_quickpicks_telegram),
        "github" to stringResource(R.string.onboarding_quickpicks_github),
        "mastodon" to stringResource(R.string.onboarding_quickpicks_mastodon),
        "proton" to stringResource(R.string.onboarding_quickpicks_proton),
        "bitwarden" to stringResource(R.string.onboarding_quickpicks_bitwarden),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Progress bars — 6 bars, height 4dp, gap 6dp, horizontal padding 24dp
        ProgressBars(
            pageCount = PAGE_COUNT,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceSm),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> WelcomePage(onLanguageChange = onLanguageChange)
                1 -> WhatPage()
                2 -> AppearancePage(
                    themeMode = state.themeMode,
                    accentColor = state.accentColor,
                    onThemeMode = viewModel::setThemeMode,
                    onAccentColor = viewModel::setAccentColor,
                )

                3 -> SecurityPage(
                    passwordSet = state.passwordSet,
                    password = secPassword,
                    confirm = secConfirm,
                    onPasswordChange = { secPassword = it },
                    onConfirmChange = { secConfirm = it },
                )

                4 -> BackupPage(
                    backupEnabled = state.backupEnabled,
                    directoryUri = state.backupDirectoryUri,
                    schedule = state.backupSchedule,
                    onToggleBackup = viewModel::setBackupEnabled,
                    onDirectoryUri = viewModel::setBackupDirectoryUri,
                    onSchedule = viewModel::setBackupSchedule,
                )

                5 -> QuickPicksPage(
                    picked = state.pickedAppIds,
                    onToggle = viewModel::togglePickedApp,
                )

                6 -> DonePage()
            }
        }

        // Global fixed footer
        OnboardingFooter(
            page = pagerState.currentPage,
            passwordSet = state.passwordSet,
            secValid = secValid,
            pickedCount = state.pickedAppIds.size,
            onSkip = { viewModel.goTo(PAGE_COUNT - 1) },
            onNext = { viewModel.goTo(pagerState.currentPage + 1) },
            onSetPassword = { viewModel.setPassword(secPassword); viewModel.goTo(4) },
            onFinish = viewModel::finish,
            quickPicksStatus = state.quickPicksStatus,
            onAddApps = {
                val apps = state.pickedAppIds.mapNotNull { id ->
                    val name = suggestionNames[id] ?: return@mapNotNull null
                    val url = SUGGESTION_URLS[id] ?: return@mapNotNull null
                    name to url
                }
                viewModel.addPickedApps(apps)
            },
            onCancelQuickPicks = {
                viewModel.cancelQuickPicks()
                viewModel.goTo(pagerState.currentPage + 1)
            },
        )

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

// ── Progress bars ─────────────────────────────────────────────────────────────

@Composable
private fun ProgressBars(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        repeat(pageCount) { i ->
            val barModifier = Modifier
                .weight(1f)
                .height(Dimens.spaceXxs)
                .clip(RoundedCornerShape(Dimens.cornerXxs))

            when {
                i < currentPage -> {
                    // Done: solid primary
                    Box(barModifier.background(primary))
                }

                i == currentPage -> {
                    // Active: horizontal gradient primary → primaryContainer
                    Box(
                        barModifier.background(
                            Brush.horizontalGradient(listOf(primary, primaryContainer))
                        )
                    )
                }

                else -> {
                    // Future: surfaceVariant
                    Box(barModifier.background(surfaceVariant))
                }
            }
        }
    }
}

// ── Global footer ─────────────────────────────────────────────────────────────

@Composable
private fun OnboardingFooter(
    page: Int,
    passwordSet: Boolean,
    secValid: Boolean,
    pickedCount: Int,
    quickPicksStatus: QuickPicksStatus,
    onAddApps: () -> Unit,
    onCancelQuickPicks: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onSetPassword: () -> Unit,
    onFinish: () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
    ) {
        when (page) {
            0 -> FooterRow(
                secondaryLabel = stringResource(R.string.onboarding_skip_setup),
                onSecondary = onSkip,
                primaryLabel = stringResource(R.string.common_continue),
                onPrimary = onNext,
                showPrimaryArrow = true,
            )

            1, 2, 4 -> FooterRow(
                secondaryLabel = null,
                onSecondary = null,
                primaryLabel = stringResource(R.string.common_continue),
                onPrimary = onNext,
                showPrimaryArrow = true,
            )

            3 -> if (passwordSet) {
                FooterRow(
                    secondaryLabel = null,
                    onSecondary = null,
                    primaryLabel = stringResource(R.string.common_continue),
                    onPrimary = onNext,
                    showPrimaryArrow = true,
                )
            } else if (secValid) {
                FooterRow(
                    secondaryLabel = stringResource(R.string.onboarding_skip_for_now),
                    onSecondary = onNext,
                    primaryLabel = stringResource(R.string.onboarding_security_set),
                    onPrimary = onSetPassword,
                    showPrimaryArrow = true,
                )
            } else {
                FooterRow(
                    secondaryLabel = null,
                    onSecondary = null,
                    primaryLabel = stringResource(R.string.onboarding_skip_for_now),
                    onPrimary = onNext,
                    showPrimaryArrow = false,
                )
            }

            5 -> when (quickPicksStatus) {
                QuickPicksStatus.Idle -> if (pickedCount > 0) {
                    FooterRow(
                        secondaryLabel = stringResource(R.string.onboarding_skip_for_now),
                        onSecondary = onNext,
                        primaryLabel = if (pickedCount == 1)
                            stringResource(R.string.onboarding_quickpicks_add_one)
                        else
                            stringResource(R.string.onboarding_quickpicks_add_n, pickedCount),
                        onPrimary = onAddApps,
                        showPrimaryArrow = true,
                    )
                } else {
                    FooterRow(
                        secondaryLabel = null,
                        onSecondary = null,
                        primaryLabel = stringResource(R.string.onboarding_skip_for_now),
                        onPrimary = onNext,
                        showPrimaryArrow = false,
                    )
                }

                is QuickPicksStatus.Adding -> {
                    val status = quickPicksStatus as QuickPicksStatus.Adding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    ) {
                        OutlinedButton(
                            onClick = onCancelQuickPicks,
                            modifier = Modifier.heightIn(min = Dimens.sizeApp),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = Dimens.sizeMd
                            ),
                        ) {
                            Text(stringResource(R.string.onboarding_skip_for_now))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.height(Dimens.sizeApp),
                            shape = CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = Dimens.spaceXl
                            ),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.sizeSm),
                                strokeWidth = Dimens.borderSelected,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(Dimens.spaceSm))
                            Text("${status.done} / ${status.total}")
                        }
                    }
                }

                QuickPicksStatus.Done -> FooterRow(
                    secondaryLabel = null,
                    onSecondary = null,
                    primaryLabel = stringResource(R.string.common_continue),
                    onPrimary = onNext,
                    showPrimaryArrow = true,
                )
            }

            6 -> FooterRow(
                secondaryLabel = null,
                onSecondary = null,
                primaryLabel = stringResource(R.string.onboarding_get_started),
                onPrimary = onFinish,
                showPrimaryArrow = false,
            )
        }
    }
}

// ── Shared page layout ────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageLayout(
    hero: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (hero != null) {
            hero()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spaceLg, vertical = 20.dp),
        ) {
            content()
        }
    }
}

// ── Shared footer row ─────────────────────────────────────────────────────────

@Composable
private fun FooterRow(
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    showPrimaryArrow: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier.heightIn(min = Dimens.sizeApp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.sizeMd),
            ) {
                Text(secondaryLabel)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onPrimary,
            modifier = Modifier.height(Dimens.sizeApp),
            shape = CircleShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Dimens.spaceXl),
        ) {
            Text(primaryLabel)
            if (showPrimaryArrow) {
                Spacer(Modifier.width(Dimens.spaceSm))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.sizeSm),
                )
            }
        }
    }
}

// ── Page 1: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(
    onLanguageChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.getLanguageCode(context)) }

    OnboardingPageLayout(
        hero = { WelcomeHero() },
    ) {
        // Title with "Shellify" in primary color
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.onboarding_welcome_to) + " ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Shellify")
                }
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.space22))

        // "LANGUAGE" eyebrow
        Text(
            text = stringResource(R.string.onboarding_language_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = Dimens.letterSpacingOverline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))

        // 2-column grid of language cards
        val languages = listOf(
            "en" to "English",
            "fr" to "Français",
            "ar" to "العربية",
        )
        // Pair into rows of 2
        val rows = languages.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    rowItems.forEach { (code, label) ->
                        val isSelected = selectedLanguage == code
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(Dimens.corner14))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .border(
                                    width = if (isSelected) Dimens.borderSelected else Dimens.borderDefault,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(Dimens.corner14),
                                )
                                .clickable(enabled = !isSelected) {
                                    selectedLanguage = code
                                    onLanguageChange(code)
                                }
                                .padding(horizontal = Dimens.spaceMd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                            Spacer(Modifier.width(Dimens.spaceSm))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else androidx.compose.ui.graphics.Color.Transparent,
                                modifier = Modifier.size(Dimens.sizeXs),
                            )
                        }
                    }
                    // If odd number, add empty spacer
                    if (rowItems.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Welcome Hero ──────────────────────────────────────────────────────────────

@Composable
private fun WelcomeHero() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val background = MaterialTheme.colorScheme.background

    val tr = rememberInfiniteTransition(label = "welcome_hero")
    val bobY by tr.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.verticalGradient(
                    listOf(background, primaryContainer.copy(alpha = 0.3f)),
                ),
            )
            .clip(RoundedCornerShape(bottomStart = Dimens.corner28, bottomEnd = Dimens.corner28)),
        contentAlignment = Alignment.Center,
    ) {
        // Floating tile constellation
        data class FloatingTile(
            val offsetX: Dp, val offsetY: Dp,
            val size: Dp, val icon: ImageVector, val phaseOffset: Float,
        )

        val tiles = listOf(
            FloatingTile((-130).dp, (-80).dp, 48.dp, Icons.Default.GTranslate, 0.0f),
            FloatingTile(110.dp, (-90).dp, 44.dp, Icons.Default.Shield, 0.3f),
            FloatingTile((-110).dp, 60.dp, 52.dp, Icons.Default.Lock, 0.6f),
            FloatingTile(120.dp, 50.dp, 40.dp, Icons.Default.Palette, 0.9f),
            FloatingTile((-60).dp, (-110).dp, 44.dp, Icons.Default.Apps, 1.2f),
            FloatingTile(70.dp, 90.dp, 48.dp, Icons.Default.PhoneAndroid, 1.5f),
        )

        tiles.forEach { tile ->
            val yOff = bobY * cos(tile.phaseOffset * PI.toFloat()).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = tile.offsetX, y = tile.offsetY + yOff.dp)
                    .size(tile.size)
                    .clip(RoundedCornerShape(Dimens.corner14))
                    .background(primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tile.icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(tile.size * 0.5f),
                )
            }
        }

        // Central brand tile
        Box(
            modifier = Modifier
                .offset(y = bobY.dp)
                .size(Dimens.sizeEmptyIconLg)
                .clip(RoundedCornerShape(Dimens.corner28))
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.Center,
        ) {
            // Brand "P"
            Text(
                text = "P",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            // Accent dot top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = Dimens.spaceXs, y = -Dimens.spaceXs)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB68A)),
            )
        }
    }
}

// ── Page 2: Features ──────────────────────────────────────────────────────────

private data class Feature(
    val icon: ImageVector,
    val bgTint: @Composable () -> Color,
    val titleRes: Int,
    val descRes: Int,
)

@Composable
private fun WhatPage() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val features = listOf(
        Feature(
            Icons.Default.Apps,
            { primaryContainer },
            R.string.onboarding_feature_apps_title,
            R.string.onboarding_feature_apps_desc
        ),
        Feature(
            Icons.Default.Shield,
            { primaryContainer.copy(alpha = 0.5f) },
            R.string.onboarding_feature_adblock_title,
            R.string.onboarding_feature_adblock_desc
        ),
        Feature(
            Icons.Default.Layers,
            { surfaceVariant },
            R.string.onboarding_feature_isolation_title,
            R.string.onboarding_feature_isolation_desc
        ),
        Feature(
            Icons.Default.GTranslate,
            { primaryContainer },
            R.string.onboarding_feature_translate_title,
            R.string.onboarding_feature_translate_desc
        ),
        Feature(
            Icons.Default.Notifications,
            { primaryContainer.copy(alpha = 0.5f) },
            R.string.onboarding_feature_notifications_title,
            R.string.onboarding_feature_notifications_desc
        ),
        Feature(
            Icons.Default.VpnLock,
            { surfaceVariant },
            R.string.onboarding_feature_tor_title,
            R.string.onboarding_feature_tor_desc
        ),
    )

    OnboardingPageLayout(
        hero = null,
    ) {
        // "FOUR THINGS" eyebrow pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.cornerFull))
                .background(primaryContainer)
                .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(Dimens.space14),
            )
            Text(
                text = stringResource(R.string.onboarding_what_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = primary,
            )
        }

        Spacer(Modifier.height(Dimens.space14))

        Text(
            text = stringResource(R.string.onboarding_what_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Dimens.space22))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space10)) {
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            Dimens.borderDefault,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(Dimens.corner18)
                        )
                        .clip(RoundedCornerShape(Dimens.corner18))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(Dimens.space14),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    val bgColor = feature.bgTint()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .background(bgColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            feature.icon,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(Dimens.sizeLg),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(feature.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = stringResource(feature.descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Page 3: Appearance ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearancePage(
    themeMode: ThemeMode,
    accentColor: Int?,
    onThemeMode: (ThemeMode) -> Unit,
    onAccentColor: (Int?) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    OnboardingPageLayout(
        hero = null,
    ) {
        Text(
            text = stringResource(R.string.onboarding_appearance_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Dimens.spaceSm))

        Text(
            text = stringResource(R.string.onboarding_appearance_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.sizeMd))

        // Live preview card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(Dimens.borderDefault, outlineVariant, RoundedCornerShape(Dimens.corner20))
                .clip(RoundedCornerShape(Dimens.corner20))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Dimens.space18),
        ) {
            Text(
                text = stringResource(R.string.onboarding_appearance_preview_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = Dimens.letterSpacingOverline,
                color = onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.spaceMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                // Tile 0: gradient bg, "P"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.corner14))
                        .background(Brush.linearGradient(listOf(primary, secondary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "P",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Tile 1: secondaryContainer bg, "M"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.corner14))
                        .background(secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "M",
                        color = onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Tile 2: primaryContainer 60% bg, "N"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.corner14))
                        .background(primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "N",
                        color = primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Tile 3: transparent + dashed border, "+"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.corner14))
                        .border(
                            Dimens.strokeSm,
                            outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(Dimens.corner14)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+",
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(Dimens.space14))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.spaceSm)
                        .clip(RoundedCornerShape(Dimens.spaceXxs))
                        .background(surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(Dimens.spaceSm)
                            .clip(RoundedCornerShape(Dimens.spaceXxs))
                            .background(primary),
                    )
                }
                Text(
                    text = "3/5 set up",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Dimens.space22))

        // "APPEARANCE" eyebrow
        Text(
            text = stringResource(R.string.global_settings_section_appearance).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = Dimens.letterSpacingOverline,
            color = onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.spaceSm))

        // Theme mode segmented button
        val modes = listOf(
            Triple(
                ThemeMode.SYSTEM,
                Icons.Default.BrightnessAuto,
                stringResource(R.string.global_settings_theme_system)
            ),
            Triple(
                ThemeMode.LIGHT,
                Icons.Default.LightMode,
                stringResource(R.string.global_settings_theme_light)
            ),
            Triple(
                ThemeMode.DARK,
                Icons.Default.DarkMode,
                stringResource(R.string.global_settings_theme_dark)
            ),
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (mode, icon, label) ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { onThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = themeMode == mode) {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                ) { Text(label) }
            }
        }

        Spacer(Modifier.height(Dimens.space18))

        // "ACCENT COLOR" eyebrow
        Text(
            text = stringResource(R.string.global_settings_accent_color).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = Dimens.letterSpacingOverline,
            color = onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.space10))

        // 5 accent swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space10),
        ) {
            ACCENT_COLORS.forEach { colorInt ->
                val isSelected = accentColor == colorInt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(Dimens.cornerXl))
                        .background(Color(colorInt))
                        .clickable { onAccentColor(colorInt) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(Dimens.sizeMd),
                        )
                    }
                }
            }
        }
    }
}

// ── Page 4: Security ─────────────────────────────────────────────────────────

@Composable
private fun SecurityPage(
    passwordSet: Boolean,
    password: String,
    confirm: String,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
) {
    var pwVis by remember { mutableStateOf(false) }
    var cfVis by remember { mutableStateOf(false) }
    var confirmTouched by remember { mutableStateOf(false) }

    val mismatch = confirmTouched && confirm.isNotEmpty() && password != confirm

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    OnboardingPageLayout(
        hero = null,
    ) {
        // Lock icon tile
        Box(
            modifier = Modifier
                .size(Dimens.sizeIconHero)
                .clip(RoundedCornerShape(Dimens.corner18))
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(Dimens.size3xl)
            )
        }

        Spacer(Modifier.height(Dimens.space18))

        Text(
            text = stringResource(R.string.onboarding_security_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Dimens.spaceSm))

        Text(
            text = stringResource(R.string.onboarding_security_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.spaceXl))

        if (passwordSet) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Security, null, tint = primary) },
                headlineContent = { Text(stringResource(R.string.onboarding_security_set_done) + " ✓") },
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.cornerXl),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    Dimens.borderDefault,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(Dimens.spaceMd)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.common_password)) },
                        singleLine = true,
                        visualTransformation = if (pwVis) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { pwVis = !pwVis }) {
                                Icon(
                                    if (pwVis) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Dimens.spaceSm))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            onConfirmChange(it)
                            if (it.isNotEmpty()) confirmTouched = true
                        },
                        label = { Text(stringResource(R.string.common_confirm_password)) },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text("Passwords don't match") }
                        } else null,
                        visualTransformation = if (cfVis) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { cfVis = !cfVis }) {
                                Icon(
                                    if (cfVis) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Dimens.spaceMd))

                    StrengthMeter(password)
                }
            }
        }
    }
}

// ── Strength Meter ────────────────────────────────────────────────────────────

@Composable
private fun StrengthMeter(password: String) {
    val strength = minOf(4, password.length / 3)
    val barColor = when {
        strength >= 3 -> MaterialTheme.colorScheme.primary
        strength == 2 -> Color(0xFFFF8A5C)
        else -> Color(0xFFE0B341)
    }
    val labels = listOf("", "Weak", "Okay", "Good", "Strong")
    val labelColor = when {
        strength >= 3 -> MaterialTheme.colorScheme.primary
        strength == 2 -> Color(0xFFFF8A5C)
        else -> Color(0xFFE0B341)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.spaceXxs)
                    .clip(RoundedCornerShape(Dimens.cornerXxs))
                    .background(
                        if (i < strength) barColor
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }

    if (password.isNotEmpty()) {
        Spacer(Modifier.height(Dimens.spaceXs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Password strength",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (strength > 0) {
                Text(
                    text = labels[strength],
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                )
            }
        }
    }
}

// ── Page 5: Backup ────────────────────────────────────────────────────────────

@Composable
private fun BackupPage(
    backupEnabled: Boolean,
    directoryUri: String?,
    schedule: BackupSchedule,
    onToggleBackup: (Boolean) -> Unit,
    onDirectoryUri: (String) -> Unit,
    onSchedule: (BackupSchedule) -> Unit,
) {
    val context = LocalContext.current
    val folderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                onDirectoryUri(it.toString())
            }
        }
    val folderDisplay = directoryUri
        ?.let {
            android.net.Uri.decode(it).substringAfterLast(":")
                .ifEmpty { it.substringAfterLast("/") }
        }
        ?: stringResource(R.string.global_settings_backup_folder_not_selected)

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    OnboardingPageLayout(
        hero = null,
    ) {
        // Cloud icon tile
        Box(
            modifier = Modifier
                .size(Dimens.sizeIconHero)
                .clip(RoundedCornerShape(Dimens.corner18))
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(Dimens.size3xl)
            )
        }

        Spacer(Modifier.height(Dimens.space18))

        Text(
            text = stringResource(R.string.onboarding_backup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Dimens.spaceSm))

        Text(
            text = stringResource(R.string.onboarding_backup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.spaceXl))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space10)) {
            // 1. Enable card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        Dimens.borderDefault,
                        outlineVariant,
                        RoundedCornerShape(Dimens.corner18)
                    )
                    .clip(RoundedCornerShape(Dimens.corner18))
                    .background(
                        if (backupEnabled) primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface,
                    )
                    .padding(Dimens.spaceLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.sizeCard)
                        .clip(RoundedCornerShape(Dimens.cornerLg))
                        .background(primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(Dimens.sizeMd)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_backup_enable),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_backup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant,
                    )
                }
                Switch(checked = backupEnabled, onCheckedChange = onToggleBackup)
            }

            // 2. Folder card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        Dimens.borderDefault,
                        outlineVariant,
                        RoundedCornerShape(Dimens.corner18)
                    )
                    .clip(RoundedCornerShape(Dimens.corner18))
                    .background(MaterialTheme.colorScheme.surface)
                    .then(
                        if (backupEnabled) Modifier.clickable { folderLauncher.launch(null) }
                        else Modifier,
                    )
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14)
                    .then(if (!backupEnabled) Modifier.scale(1f) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimens.sizeCard)
                        .clip(RoundedCornerShape(Dimens.cornerLg))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (!backupEnabled) Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.55f
                                )
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = if (backupEnabled) primary else onSurfaceVariant,
                        modifier = Modifier.size(Dimens.sizeLg),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_backup_folder_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant,
                    )
                    Text(
                        text = folderDisplay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = if (backupEnabled) MaterialTheme.colorScheme.onSurface
                        else onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = onSurfaceVariant,
                    modifier = Modifier.size(Dimens.sizeMd),
                )
            }

            // 3. Schedule card
            val scheduleAlpha = if (!backupEnabled) 0.55f else 1f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        Dimens.borderDefault,
                        outlineVariant,
                        RoundedCornerShape(Dimens.corner18)
                    )
                    .clip(RoundedCornerShape(Dimens.corner18))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.space14),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = onSurfaceVariant.copy(alpha = scheduleAlpha),
                        modifier = Modifier.size(Dimens.sizeSm),
                    )
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceVariant.copy(alpha = scheduleAlpha),
                    )
                }

                Spacer(Modifier.height(Dimens.spaceMd))

                val scheduleOptions = listOf(
                    BackupSchedule.NONE to stringResource(R.string.onboarding_schedule_manual),
                    BackupSchedule.WEEKLY to stringResource(R.string.global_settings_schedule_weekly),
                    BackupSchedule.MONTHLY to stringResource(R.string.global_settings_schedule_monthly),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                ) {
                    scheduleOptions.forEach { (sched, label) ->
                        val isSelected = schedule == sched
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 38.dp)
                                .clip(CircleShape)
                                .border(Dimens.borderDefault, outlineVariant, CircleShape)
                                .background(
                                    if (isSelected) primary
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .then(
                                    if (backupEnabled) Modifier.clickable { onSchedule(sched) }
                                    else Modifier,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight(600) else FontWeight.Normal,
                                color = if (isSelected) Color.White
                                else onSurfaceVariant.copy(alpha = scheduleAlpha),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Page 6: QuickPicks ────────────────────────────────────────────────────────

@Composable
private fun QuickPicksHero() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val tr = rememberInfiniteTransition(label = "quickpicks_hero")
    val bobY by tr.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.heroHeightSm)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        primaryContainer.copy(alpha = 0.3f)
                    ),
                ),
            )
            .clip(RoundedCornerShape(bottomStart = Dimens.corner28, bottomEnd = Dimens.corner28)),
        contentAlignment = Alignment.Center,
    ) {
        // Radial glow
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primaryContainer.copy(alpha = 0.5f), Color.Transparent),
                    ),
                ),
        )

        data class FloatingTile(
            val offsetX: Dp, val offsetY: Dp,
            val size: Dp, val icon: ImageVector,
            val bg: Color, val fg: Color,
            val phaseOffset: Float,
        )

        val tiles = listOf(
            FloatingTile(
                (-120).dp,
                (-30).dp,
                50.dp,
                Icons.Default.Email,
                primaryContainer,
                primary,
                0.0f
            ),
            FloatingTile(
                110.dp,
                (-45).dp,
                48.dp,
                Icons.Default.Security,
                SuggestionVideoBg,
                CategoryToolsFg,
                0.5f
            ),
            FloatingTile(
                (-85).dp,
                45.dp,
                46.dp,
                Icons.Default.ChatBubble,
                SuggestionChatBg,
                SuggestionChatFg,
                1.0f
            ),
            FloatingTile(
                105.dp,
                35.dp,
                52.dp,
                Icons.Default.Edit,
                CategoryReadingBg,
                CategoryReadingFg,
                1.5f
            ),
        )

        tiles.forEach { tile ->
            val yOff = bobY * cos(tile.phaseOffset * PI.toFloat()).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = tile.offsetX, y = tile.offsetY + yOff.dp)
                    .size(tile.size)
                    .clip(RoundedCornerShape(Dimens.corner14))
                    .background(tile.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    tile.icon,
                    contentDescription = null,
                    tint = tile.fg,
                    modifier = Modifier.size(tile.size * 0.42f),
                )
            }
        }
    }
}

@Composable
private fun QuickPicksPage(
    picked: List<String>,
    onToggle: (String) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    data class Suggestion(
        val id: String,
        val name: String,
        val host: String,
        val icon: ImageVector,
        val bg: Color,
        val fg: Color,
    )

    val suggestions = listOf(
        Suggestion(
            "reddit",
            stringResource(R.string.onboarding_quickpicks_reddit),
            stringResource(R.string.onboarding_quickpicks_reddit_host),
            Icons.Default.Forum,
            CategoryReadingBg,
            CategoryReadingFg
        ),
        Suggestion(
            "discord",
            stringResource(R.string.onboarding_quickpicks_discord),
            stringResource(R.string.onboarding_quickpicks_discord_host),
            Icons.Default.Headset,
            SuggestionChatBg,
            SuggestionChatFg
        ),
        Suggestion(
            "telegram",
            stringResource(R.string.onboarding_quickpicks_telegram),
            stringResource(R.string.onboarding_quickpicks_telegram_host),
            Icons.AutoMirrored.Filled.Send,
            SuggestionVideoBg,
            CategoryToolsFg
        ),
        Suggestion(
            "github",
            stringResource(R.string.onboarding_quickpicks_github),
            stringResource(R.string.onboarding_quickpicks_github_host),
            Icons.Default.Code,
            SuggestionChatBg.copy(alpha = 0.45f),
            SuggestionChatFg
        ),
        Suggestion(
            "mastodon",
            stringResource(R.string.onboarding_quickpicks_mastodon),
            stringResource(R.string.onboarding_quickpicks_mastodon_host),
            Icons.Default.Public,
            SuggestionChatBg.copy(alpha = 0.6f),
            SuggestionChatFg
        ),
        Suggestion(
            "proton",
            stringResource(R.string.onboarding_quickpicks_proton),
            stringResource(R.string.onboarding_quickpicks_proton_host),
            Icons.Default.Email,
            primaryContainer,
            primary
        ),
        Suggestion(
            "bitwarden",
            stringResource(R.string.onboarding_quickpicks_bitwarden),
            stringResource(R.string.onboarding_quickpicks_bitwarden_host),
            Icons.Default.Security,
            SuggestionVideoBg.copy(alpha = 0.6f),
            CategoryToolsFg
        ),
    )

    OnboardingPageLayout(
        hero = { QuickPicksHero() },
    ) {
        Text(
            text = stringResource(R.string.onboarding_quickpicks_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(Dimens.spaceSm))

        Text(
            text = stringResource(R.string.onboarding_quickpicks_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.space22))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.onboarding_quickpicks_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = Dimens.letterSpacingOverline,
                color = onSurfaceVariant,
            )
            if (picked.isNotEmpty()) {
                Text(
                    text = " · ${picked.size} ${stringResource(R.string.onboarding_quickpicks_selected)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                )
            }
        }

        Spacer(Modifier.height(Dimens.spaceSm))

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space10)) {
            suggestions.forEach { s ->
                val isSelected = s.id in picked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.corner18))
                        .background(
                            if (isSelected) primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surface,
                        )
                        .border(
                            width = if (isSelected) Dimens.strokeSm else Dimens.borderDefault,
                            color = if (isSelected) primary else outlineVariant,
                            shape = RoundedCornerShape(Dimens.corner18),
                        )
                        .clickable { onToggle(s.id) }
                        .padding(horizontal = Dimens.space14, vertical = Dimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.sizeIconTile)
                            .clip(RoundedCornerShape(Dimens.cornerLg))
                            .background(s.bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            s.icon,
                            contentDescription = null,
                            tint = s.fg,
                            modifier = Modifier.size(Dimens.sizeLg),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = s.host,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(Dimens.sizeCheckPill)
                            .clip(CircleShape)
                            .background(if (isSelected) primary else Color.Transparent)
                            .border(
                                width = if (isSelected) 0.dp else Dimens.strokeSm,
                                color = if (isSelected) Color.Transparent else outlineVariant,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else onSurfaceVariant,
                            modifier = Modifier.size(Dimens.sizeXs),
                        )
                    }
                }
            }
        }
    }
}

// ── Page 7: Done ──────────────────────────────────────────────────────────────

@Composable
private fun DonePage() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val checkScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.5f),
        label = "check_scale",
    )

    OnboardingPageLayout(
        hero = { DoneHero(appeared, checkScale) },
    ) {
        Text(
            text = stringResource(R.string.onboarding_all_set),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_ready),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

    }
}

// ── Done Hero with confetti ───────────────────────────────────────────────────

@Composable
private fun DoneHero(appeared: Boolean, checkScale: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary

    val tr = rememberInfiniteTransition(label = "confetti")
    val progress by tr.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti_progress",
    )

    // 22 confetti elements with stable random properties
    data class ConfettiEl(
        val baseX: Float, val baseY: Float,
        val size: Float, val rotation: Float,
        val colorIdx: Int, val shape: Int, // 0=circle, 1=rect, 2=rounded
        val delay: Float,
    )

    val confettiEls = remember {
        val rng = java.util.Random(42L)
        List(22) {
            ConfettiEl(
                baseX = rng.nextFloat(),
                baseY = rng.nextFloat() * 0.5f - 0.3f,
                size = 6f + rng.nextFloat() * 10f,
                rotation = rng.nextFloat() * 360f,
                colorIdx = it % 5,
                shape = it % 3,
                delay = rng.nextFloat() * 0.8f,
            )
        }
    }

    val confettiColors = listOf(
        primary,
        primaryContainer,
        Color(0xFFFFB68A),
        primaryContainer.copy(alpha = 0.8f),
        secondary,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(primaryContainer.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(bottomStart = Dimens.corner28, bottomEnd = Dimens.corner28)),
        contentAlignment = Alignment.Center,
    ) {
        // Confetti via Canvas
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            confettiEls.forEach { el ->
                val p = ((progress - el.delay + 1f) % 1f).coerceIn(0f, 1f)
                val x = el.baseX * size.width
                val y = el.baseY * size.height + p * 250.dp.toPx()
                val s = el.size.dp.toPx()
                val color = confettiColors[el.colorIdx]

                when (el.shape) {
                    0 -> drawCircle(color, s / 2f, androidx.compose.ui.geometry.Offset(x, y))
                    1 -> drawRect(
                        color,
                        topLeft = androidx.compose.ui.geometry.Offset(x - s / 2f, y - s / 3f),
                        size = androidx.compose.ui.geometry.Size(s, s * 0.6f),
                    )

                    else -> drawRoundRect(
                        color,
                        topLeft = androidx.compose.ui.geometry.Offset(x - s / 2f, y - s / 2f),
                        size = androidx.compose.ui.geometry.Size(s, s),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.3f),
                    )
                }
            }
        }

        // Radial glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primaryContainer, Color.Transparent),
                    ),
                ),
        )

        // Check box
        Box(
            modifier = Modifier
                .scale(checkScale)
                .size(104.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(Dimens.sizeIconHero),
            )
        }
    }
}
