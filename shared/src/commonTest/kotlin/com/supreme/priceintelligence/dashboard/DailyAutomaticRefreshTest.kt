package com.supreme.priceintelligence.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyAutomaticRefreshTest {

    @Test
    fun attemptsAreRememberedOnlyForTheCurrentIndiaDay() {
        val nowMillis = 1_700_000_000_000L

        val storedValue =
            writeAutomaticRefreshAttempts(
                productIds = setOf(8L, 3L, 8L),
                nowMillis = nowMillis
            )

        assertEquals(
            setOf(3L, 8L),
            readAutomaticRefreshAttempts(
                storedValue = storedValue,
                nowMillis = nowMillis
            )
        )

        assertTrue(
            readAutomaticRefreshAttempts(
                storedValue = storedValue,
                nowMillis =
                    nowMillis +
                            AUTOMATIC_REFRESH_DAY_MILLIS
            ).isEmpty()
        )
    }

    @Test
    fun damagedLedgerIsHandledSafely() {
        assertTrue(
            readAutomaticRefreshAttempts(
                storedValue = "damaged",
                nowMillis = 1_700_000_000_000L
            ).isEmpty()
        )

        assertTrue(
            readAutomaticRefreshAttempts(
                storedValue = "wrong|abc,-1,0",
                nowMillis = 1_700_000_000_000L
            ).isEmpty()
        )
    }
}