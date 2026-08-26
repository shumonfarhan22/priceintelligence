package com.supreme.priceintelligence.dashboard

import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.settings.IosAppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IosDailyPriceRefreshTask {
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default
        )

    private var runningJob: Job? = null

    fun start(
        onCompleted: () -> Unit,
        onNeedsMoreTime: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (runningJob?.isActive == true) {
            onFailed()
            return
        }

        runningJob = scope.launch {
            val database =
                getRoomDatabase(
                    getDatabaseBuilder()
                )

            val scraper =
                PriceScraper()

            try {
                val refresh =
                    DailyBackgroundPriceRefresh(
                        repository =
                            InventoryRepository(
                                database.inventoryDao()
                            ),
                        scraper =
                            scraper,
                        preferences =
                            IosAppPreferences(),
                        notifier =
                            IosPriceChangeNotifier()
                    )

                val result =
                    refresh.runBatch(
                        maximumProducts = 3,
                        maximumRuntimeMillis =
                            4L * 60L * 1000L
                    )

                if (result.completed) {
                    onCompleted()
                } else {
                    onNeedsMoreTime()
                }
            } catch (error: CancellationException) {
                onFailed()
                throw error
            } catch (_: Exception) {
                onFailed()
            } finally {
                scraper.close()
                database.close()
                runningJob = null
            }
        }
    }

    fun cancel() {
        runningJob?.cancel()
    }
}