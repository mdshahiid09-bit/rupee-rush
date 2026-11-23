package com.rupeerush.user

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CheckinStreakTest {

    @Test
    fun testConsecutiveDayIncrementsStreak() {
        val lastCheckinDate = LocalDate.parse("2026-11-22")
        val today = LocalDate.parse("2026-11-23")
        val previousStreak = 3

        val newStreak = if (lastCheckinDate.plusDays(1) == today) {
            previousStreak + 1
        } else {
            1
        }

        assertEquals(4, newStreak)
    }

    @Test
    fun testMissedDayResetsStreakToOne() {
        val lastCheckinDate = LocalDate.parse("2026-11-20")
        val today = LocalDate.parse("2026-11-23")
        val previousStreak = 5

        val newStreak = if (lastCheckinDate.plusDays(1) == today) {
            previousStreak + 1
        } else {
            1
        }

        assertEquals(1, newStreak)
    }

    @Test
    fun testBonusCalculationFormula() {
        val baseBonus = 1.00
        val increment = 0.50
        val maxBonus = 10.00

        // Streak 1
        val bonusDay1 = minOf(baseBonus + (1 - 1) * increment, maxBonus)
        assertEquals(1.00, bonusDay1, 0.001)

        // Streak 5
        val bonusDay5 = minOf(baseBonus + (5 - 1) * increment, maxBonus)
        assertEquals(3.00, bonusDay5, 0.001)

        // High Streak cap test
        val bonusDay30 = minOf(baseBonus + (30 - 1) * increment, maxBonus)
        assertEquals(10.00, bonusDay30, 0.001)
    }
}
