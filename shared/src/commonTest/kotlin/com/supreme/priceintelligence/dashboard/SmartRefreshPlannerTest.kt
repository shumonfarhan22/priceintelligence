package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartRefreshPlannerTest {

    @Test
    fun profileRoundTripPreservesSafeBoundedValues() {
        val nowMillis = 1_700_000_000_000L

        val firstFailure =
            updateSmartRefreshOutcome(
                records = emptyMap(),
                productId = 7L,
                succeeded = false,
                priceMoved = false,
                nowMillis = nowMillis
            )

        val secondFailure =
            updateSmartRefreshOutcome(
                records = firstFailure,
                productId = 7L,
                succeeded = false,
                priceMoved = false,
                nowMillis =
                    nowMillis +
                            AUTOMATIC_REFRESH_DAY_MILLIS
            )

        assertEquals(
            2,
            secondFailure.getValue(7L)
                .consecutiveFailures
        )

        val recovered =
            updateSmartRefreshOutcome(
                records = secondFailure,
                productId = 7L,
                succeeded = true,
                priceMoved = true,
                nowMillis =
                    nowMillis +
                            AUTOMATIC_REFRESH_DAY_MILLIS * 2L
            )

        assertEquals(
            0,
            recovered.getValue(7L)
                .consecutiveFailures
        )
        assertEquals(
            2,
            recovered.getValue(7L)
                .volatilityPoints
        )

        val encoded =
            writeSmartRefreshProfile(recovered)

        assertEquals(
            recovered,
            readSmartRefreshProfile(encoded)
        )
        assertTrue(
            readSmartRefreshProfile("damaged")
                .isEmpty()
        )
    }

    @Test
    fun plannerPrioritizesWithoutRemovingDailyCoverage() {
        val nowMillis = 1_700_000_000_000L

        val unseen = product(
            id = 1L,
            name = "Unseen product",
            searchCount = 0,
            checkedAt = null
        )

        val popular = product(
            id = 2L,
            name = "Popular product",
            searchCount = 20,
            checkedAt =
                nowMillis -
                        AUTOMATIC_REFRESH_DAY_MILLIS * 2L
        )

        val quiet = product(
            id = 3L,
            name = "Quiet product",
            searchCount = 0,
            checkedAt =
                nowMillis -
                        AUTOMATIC_REFRESH_DAY_MILLIS * 4L
        )

        val repeatedlyFailing = product(
            id = 4L,
            name = "Failing product",
            searchCount = 30,
            checkedAt =
                nowMillis -
                        AUTOMATIC_REFRESH_DAY_MILLIS * 10L
        )

        val profile = mapOf(
            4L to SmartRefreshRecord(
                productId = 4L,
                consecutiveFailures = 2,
                lastOutcomeAt =
                    nowMillis -
                            AUTOMATIC_REFRESH_DAY_MILLIS
            )
        )

        val plan =
            buildSmartRefreshPlan(
                products = listOf(
                    quiet,
                    repeatedlyFailing,
                    popular,
                    unseen
                ),
                profile = profile,
                attemptedProductIds =
                    emptySet(),
                nowMillis = nowMillis
            )

        assertEquals(
            setOf(1L, 2L, 3L, 4L),
            plan.map { item -> item.id }.toSet()
        )

        assertEquals(
            1L,
            plan.first().id
        )

        assertTrue(
            plan.indexOfFirst { item ->
                item.id == popular.id
            } <
                plan.indexOfFirst { item ->
                    item.id == quiet.id
                }
        )
    }

    @Test
    fun attemptedProductsAreSkippedButAllOthersRemain() {
        val nowMillis = 1_700_000_000_000L

        val products = listOf(
            product(
                id = 1L,
                name = "First",
                searchCount = 10,
                checkedAt = null
            ),
            product(
                id = 2L,
                name = "Second",
                searchCount = 9,
                checkedAt = null
            ),
            product(
                id = 3L,
                name = "Third",
                searchCount = 8,
                checkedAt = null
            )
        )

        val plan =
            buildSmartRefreshPlan(
                products = products,
                profile = emptyMap(),
                attemptedProductIds =
                    setOf(1L),
                nowMillis = nowMillis
            )

        assertEquals(
            listOf(2L, 3L),
            plan.map { item -> item.id }
        )
    }

    @Test
    fun meaningfulMovementRequiresAtLeastOnePercent() {
        assertFalse(
            hasMeaningfulPriceMovement(
                oldPrice = 1_000.0,
                newPrice = 1_005.0
            )
        )

        assertTrue(
            hasMeaningfulPriceMovement(
                oldPrice = 1_000.0,
                newPrice = 1_020.0
            )
        )

        assertFalse(
            hasMeaningfulPriceMovement(
                oldPrice = null,
                newPrice = 1_020.0
            )
        )
    }

    @Test
    fun automaticSpacingStaysInsideTheSafeRange() {
        val delayMillis =
            smartRefreshSpacingMillis(
                productId = 42L,
                nowMillis = 1_700_000_000_000L
            )

        assertTrue(
            delayMillis in 16_000L..25_000L
        )
    }

    private fun product(
        id: Long,
        name: String,
        searchCount: Int,
        checkedAt: Long?
    ): InventoryItem =
        InventoryItem(
            id = id,
            productName = name,
            shopPrice = 1_000.0,
            amazonUrl =
                "https://amazon.in/product-$id",
            searchCount = searchCount,
            createdAt = 1L,
            updatedAt = 1L,
            amazonLastPrice =
                checkedAt?.let { 900.0 },
            amazonLastChecked = checkedAt
        )
}