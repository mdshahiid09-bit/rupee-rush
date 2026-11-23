package com.rupeerush.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletMathTest {

    @Test
    fun testCreditAdditionAndBalanceInvariant() {
        var balance = 100.0
        val creditAmount = 25.50
        balance += creditAmount

        assertEquals(125.50, balance, 0.001)
        assertTrue("Balance must remain non-negative", balance >= 0)
    }

    @Test
    fun testDebitSubtractionSucceedsWhenSufficientBalance() {
        var balance = 200.0
        val debitAmount = 75.0

        assertTrue("User can afford debit", balance >= debitAmount)
        balance -= debitAmount

        assertEquals(125.0, balance, 0.001)
    }

    @Test
    fun testDebitBlockedWhenInsufficientBalance() {
        val balance = 40.0
        val requestedWithdrawal = 50.0

        val canWithdraw = balance >= requestedWithdrawal
        assertFalse("Cannot withdraw more than available balance", canWithdraw)
    }
}
