package io.shellify.app.navigation

import io.shellify.app.core.theme.ThemeManager
import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.navigation.resolveStartDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    // ── Consent gate ──────────────────────────────────────────────────────────

    @Test
    fun freshInstall_routesToConsent() {
        assertEquals(
            Screen.Consent.route,
            resolveStartDestination(consentVersion = 0, onboardingDone = false, whatsNewVersion = 0),
        )
    }

    @Test
    fun freshInstall_consentTakesPrecedenceOverOnboarding() {
        assertEquals(
            Screen.Consent.route,
            resolveStartDestination(consentVersion = 0, onboardingDone = true, whatsNewVersion = 1),
        )
    }

    @Test
    fun outdatedConsent_routesToUpdateConsent() {
        assertEquals(
            Screen.UpdateConsent.route,
            resolveStartDestination(consentVersion = 1, onboardingDone = true, whatsNewVersion = 1),
        )
    }

    @Test
    fun outdatedConsent_onboardingPending_stillRoutesToUpdateConsent() {
        assertEquals(
            Screen.UpdateConsent.route,
            resolveStartDestination(consentVersion = 1, onboardingDone = false, whatsNewVersion = 0),
        )
    }

    // ── Onboarding gate ───────────────────────────────────────────────────────

    @Test
    fun currentConsent_onboardingPending_routesToOnboarding() {
        assertEquals(
            Screen.Onboarding.route,
            resolveStartDestination(
                consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                onboardingDone = false,
                whatsNewVersion = 0,
            ),
        )
    }

    // ── What's New gate ───────────────────────────────────────────────────────

    @Test
    fun consentAndOnboardingDone_whatsNewUnseen_routesToWhatsNew() {
        assertEquals(
            Screen.WhatsNew.route,
            resolveStartDestination(
                consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                onboardingDone = true,
                whatsNewVersion = 0,
            ),
        )
    }

    @Test
    fun whatsNewGate_skippedWhenOnboardingNotDone() {
        // Onboarding gate takes priority over What's New gate.
        assertEquals(
            Screen.Onboarding.route,
            resolveStartDestination(
                consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                onboardingDone = false,
                whatsNewVersion = 0,
            ),
        )
    }

    @Test
    fun whatsNewGate_skippedWhenConsentOutdated() {
        // Consent gate takes priority over What's New gate.
        assertEquals(
            Screen.UpdateConsent.route,
            resolveStartDestination(
                consentVersion = 1,
                onboardingDone = true,
                whatsNewVersion = 0,
            ),
        )
    }

    // ── Home ──────────────────────────────────────────────────────────────────

    @Test
    fun allGatesPassed_routesToHome() {
        assertEquals(
            Screen.Home.route,
            resolveStartDestination(
                consentVersion = ThemeManager.CURRENT_CONSENT_VERSION,
                onboardingDone = true,
                whatsNewVersion = ThemeManager.CURRENT_WHATS_NEW_VERSION,
            ),
        )
    }
}
