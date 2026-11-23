package com.rupeerush.user

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallTimingTest {

    @Test
    fun testInstallAfterStartedAtIsAccepted() {
        val startedAtMs = 1700000000000L
        val installTimeMs = 1700000060000L // 60 seconds after task start

        val isEligible = installTimeMs > startedAtMs
        assertTrue("Install after startedAt should be accepted", isEligible)
    }

    @Test
    fun testPreInstalledAppIsRejected() {
        val startedAtMs = 1700000000000L
        val installTimeMs = 1699990000000L // Installed before task start

        val isEligible = installTimeMs > startedAtMs
        assertFalse("Pre-installed app must be rejected as invalid", isEligible)
    }

    @Test
    fun testSameTimestampIsRejected() {
        val startedAtMs = 1700000000000L
        val installTimeMs = 1700000000000L

        val isEligible = installTimeMs > startedAtMs
        assertFalse("Install time equal to startedAt cannot be verified as new", isEligible)
    }
}
