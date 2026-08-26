@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.network.PriceFetcher
import com.supreme.priceintelligence.settings.AppPreferences
import com.supreme.priceintelligence.settings.readAppCustomization
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class DailyPriceRefreshBatchResult(
    val checkedProducts: Int,
    val remainingProducts: Int
) {
    val completed: Boolean
        get() = remainingProducts == 0
}

class DailyBackgroundPriceRefresh(
    private val repository: InventoryRepository,
    private val scraper: PriceFetcher,
    private val preferences: AppPreferences,
    private val notifier: PriceChangeNotifier
) {
    suspend fun runBatch(
        maximumProducts: Int,
        maximumRuntimeMillis: Long
    ): DailyPriceRefreshBatchResult {
        val safeMaximumProducts =
            maximumProducts.coerceIn(1, 8)

        val safeMaximumRuntime =
            maximumRuntimeMillis.coerceIn(
                30_000L,
                8L * 60L * 1000L
            )

        val startedAt =
            Clock.System.now().toEpochMilliseconds()

        val initialNow = startedAt
        val attemptedAtStart =
            readAutomaticRefreshAttempts(
                storedValue =
                    preferences
                        .automaticPriceRefreshLedger,
                nowMillis = initialNow
            )

        val plan = buildSmartRefreshPlan(
            products = repository.getAllMatching(""),
            profile = readSmartRefreshProfile(
                preferences.smartRefreshProfile
            ),
            attemptedProductIds =
                attemptedAtStart,
            nowMillis = initialNow
        )

        var checkedProducts = 0

        for (item in plan) {
            if (checkedProducts >= safeMaximumProducts) {
                break
            }

            val now =
                Clock.System.now().toEpochMilliseconds()

            if (now - startedAt >= safeMaximumRuntime) {
                break
            }

            val alreadyAttempted =
                item.id in
                        readAutomaticRefreshAttempts(
                            storedValue =
                                preferences
                                    .automaticPriceRefreshLedger,
                            nowMillis = now
                        )

            if (alreadyAttempted) {
                continue
            }

            val outcome =
                PriceRefreshCoordinator.runAutomatic {
                    val result = refreshOne(item)

                    rememberOutcome(
                        item = item,
                        outcome = result
                    )

                    markAttempt(item.id)
                    result
                }
                    ?: break

            checkedProducts += 1

            if (
                preferences
                    .priceChangeNotificationsEnabled &&
                outcome.changes.isNotEmpty()
            ) {
                try {
                    val alertChanges =
                        filterPriceChangesForAlerts(
                            changes = outcome.changes,
                            customization =
                                readAppCustomization(
                                    preferences
                                        .customizationProfile
                                )
                        )

                    if (alertChanges.isNotEmpty()) {
                        notifier.publishPriceChanges(
                            alertChanges
                        )
                    }
                } catch (_: Exception) {
                    // Saving the prices is more important than notification delivery.
                }
            }

            val afterCheck =
                Clock.System.now().toEpochMilliseconds()

            val spacingMillis =
                smartRefreshSpacingMillis(
                    productId = item.id,
                    nowMillis = afterCheck
                )

            if (
                checkedProducts < safeMaximumProducts &&
                afterCheck - startedAt + spacingMillis <
                safeMaximumRuntime
            ) {
                delay(spacingMillis.milliseconds)
            }
        }

        val finishedAt =
            Clock.System.now().toEpochMilliseconds()

        val attemptedAtFinish =
            readAutomaticRefreshAttempts(
                storedValue =
                    preferences
                        .automaticPriceRefreshLedger,
                nowMillis = finishedAt
            )

        val remaining =
            repository.getAllMatching("")
                .count { item ->
                    item.hasLinkedRetailer() &&
                            item.id !in attemptedAtFinish
                }

        return DailyPriceRefreshBatchResult(
            checkedProducts = checkedProducts,
            remainingProducts = remaining
        )
    }

    private suspend fun refreshOne(
        item: InventoryItem
    ): BackgroundRefreshOutcome {
        return try {
            val (amazonResult, flipkartResult) =
                coroutineScope {
                    val amazon = async {
                        item.amazonUrl
                            ?.takeIf(String::isNotBlank)
                            ?.let { url ->
                                scraper.fetchPrice(url)
                            }
                    }

                    val flipkart = async {
                        item.flipkartUrl
                            ?.takeIf(String::isNotBlank)
                            ?.let { url ->
                                scraper.fetchPrice(url)
                            }
                    }

                    amazon.await() to flipkart.await()
                }

            val checkedAt =
                Clock.System.now().toEpochMilliseconds()

            val detectedChanges =
                detectPriceChanges(
                    item = item,
                    amazonPrice = amazonResult?.price,
                    flipkartPrice =
                        flipkartResult?.price,
                    detectedAt = checkedAt
                )

            val savedChanges =
                mutableListOf<DetectedPriceChange>()

            amazonResult?.price?.let { price ->
                if (
                    repository.recordPriceCheck(
                        itemId = item.id,
                        retailer = PriceRetailer.AMAZON,
                        price = price,
                        checkedAt = checkedAt
                    )
                ) {
                    detectedChanges
                        .firstOrNull { change ->
                            change.retailer ==
                                    PriceRetailer.AMAZON
                        }
                        ?.let(savedChanges::add)
                }
            }

            flipkartResult?.price?.let { price ->
                if (
                    repository.recordPriceCheck(
                        itemId = item.id,
                        retailer = PriceRetailer.FLIPKART,
                        price = price,
                        checkedAt = checkedAt
                    )
                ) {
                    detectedChanges
                        .firstOrNull { change ->
                            change.retailer ==
                                    PriceRetailer.FLIPKART
                        }
                        ?.let(savedChanges::add)
                }
            }

            val preferredImage =
                selectPreferredProductImageUrl(
                    savedImageUrl = item.imageUrl,
                    amazonImageUrl = amazonResult?.image,
                    flipkartImageUrl =
                        flipkartResult?.image
                )

            if (
                preferredImage != null &&
                preferredImage != item.imageUrl
            ) {
                repository.updateImageUrl(
                    itemId = item.id,
                    imageUrl = preferredImage
                )
            }

            BackgroundRefreshOutcome(
                succeeded =
                    amazonResult?.price != null ||
                            flipkartResult?.price != null,
                priceMoved =
                    hasMeaningfulPriceMovement(
                        oldPrice = item.amazonLastPrice,
                        newPrice = amazonResult?.price
                    ) ||
                            hasMeaningfulPriceMovement(
                                oldPrice =
                                    item.flipkartLastPrice,
                                newPrice =
                                    flipkartResult?.price
                            ),
                changes = savedChanges,
                checkedAt = checkedAt
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            BackgroundRefreshOutcome(
                succeeded = false,
                priceMoved = false,
                changes = emptyList(),
                checkedAt =
                    Clock.System.now()
                        .toEpochMilliseconds()
            )
        }
    }

    private fun rememberOutcome(
        item: InventoryItem,
        outcome: BackgroundRefreshOutcome
    ) {
        val current =
            readSmartRefreshProfile(
                preferences.smartRefreshProfile
            )

        preferences.smartRefreshProfile =
            writeSmartRefreshProfile(
                updateSmartRefreshOutcome(
                    records = current,
                    productId = item.id,
                    succeeded =
                        outcome.succeeded,
                    priceMoved =
                        outcome.priceMoved,
                    nowMillis =
                        outcome.checkedAt
                )
            )
    }

    private fun markAttempt(productId: Long) {
        val now =
            Clock.System.now().toEpochMilliseconds()

        val attempts =
            readAutomaticRefreshAttempts(
                storedValue =
                    preferences
                        .automaticPriceRefreshLedger,
                nowMillis = now
            ) + productId

        preferences.automaticPriceRefreshLedger =
            writeAutomaticRefreshAttempts(
                productIds = attempts,
                nowMillis = now
            )
    }
}

private data class BackgroundRefreshOutcome(
    val succeeded: Boolean,
    val priceMoved: Boolean,
    val changes: List<DetectedPriceChange>,
    val checkedAt: Long
)

private fun InventoryItem.hasLinkedRetailer(): Boolean =
    !amazonUrl.isNullOrBlank() ||
            !flipkartUrl.isNullOrBlank()