package io.shellify.app.navigation

import io.shellify.app.presentation.navigation.Screen
import io.shellify.app.presentation.navigation.resolveStartDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    @Test
    fun freshInstall_routesToConsent() {
        assertEquals(
            Screen.Consent.route,
            resolveStartDestination(consentGiven = false, onboardingDone = false),
        )
    }

    @Test
    fun consentGiven_onboardingPending_routesToOnboarding() {
        assertEquals(
            Screen.Onboarding.route,
            resolveStartDestination(consentGiven = true, onboardingDone = false),
        )
    }

    @Test
    fun bothDone_routesToHome() {
        assertEquals(
            Screen.Home.route,
            resolveStartDestination(consentGiven = true, onboardingDone = true),
        )
    }

    @Test
    fun consent_takesHighestPrecedence_overOnboarding() {
        // Even if onboardingDone were somehow true, missing consent still gates the app.
        assertEquals(
            Screen.Consent.route,
            resolveStartDestination(consentGiven = false, onboardingDone = true),
        )
    }
}
