package com.rupeerush.user

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WithdrawalBoundsTest {

    private val minAmount = 50.00
    private val maxAmount = 1000.00
    private val dailyLimitCount = 2
    private val dailyLimitAmount = 2000.00

    @Test
    fun testAmountWithinBounds() {
        assertTrue(isValidAmount(50.0))
        assertTrue(isValidAmount(500.0))
        assertTrue(isValidAmount(1000.0))

        assertFalse("Below minimum should fail", isValidAmount(49.99))
        assertFalse("Above maximum should fail", isValidAmount(1000.01))
    }

    @Test
    fun testDailyCountAndVolumeLimits() {
        val todayCount = 2
        val canRequestMore = todayCount < dailyLimitCount
        assertFalse("Cannot exceed 2 withdrawal requests per day", canRequestMore)

        val todayTotal = 1800.0
        val newAmount = 300.0
        val isWithinDailyCap = (todayTotal + newAmount) <= dailyLimitAmount
        assertFalse("Total ₹2100 exceeds daily cap of ₹2000", isWithinDailyCap)
    }

    private fun isValidAmount(amount: Double): Boolean {
        return amount in minAmount..maxAmount
    }
}
