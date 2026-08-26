package com.supreme.priceintelligence

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.supreme.priceintelligence.dashboard.AndroidPriceChangeNotifier
import com.supreme.priceintelligence.dashboard.DailyBackgroundPriceRefresh
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getDatabaseBuilder
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.settings.AndroidAppPreferences
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class DailyPriceRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {
    override suspend fun doWork(): Result {
        val database = getRoomDatabase(
            getDatabaseBuilder(applicationContext)
        )
        val scraper = PriceScraper()

        return try {
            val refresh = DailyBackgroundPriceRefresh(
                repository = InventoryRepository(
                    database.inventoryDao()
                ),
                scraper = scraper,
                preferences = AndroidAppPreferences(
                    applicationContext
                ),
                notifier = AndroidPriceChangeNotifier(
                    applicationContext
                )
            )

            val batch = refresh.runBatch(
                maximumProducts = 4,
                maximumRuntimeMillis =
                    6L * 60L * 1000L
            )

            if (!batch.completed) {
                DailyPriceRefreshScheduler
                    .scheduleContinuation(
                        applicationContext
                    )
            }

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                DailyPriceRefreshScheduler
                    .scheduleContinuation(
                        applicationContext
                    )

                Result.success()
            }
        } finally {
            scraper.close()
            database.close()
        }
    }
}

object DailyPriceRefreshScheduler {
    private const val PERIODIC_WORK_NAME =
        "daily-price-history-refresh"

    private const val CONTINUATION_WORK_NAME =
        "daily-price-history-continuation"

    private val networkConstraint =
        Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )
            .build()

    fun ensureScheduled(context: Context) {
        val dailyWork =
            PeriodicWorkRequestBuilder<
                    DailyPriceRefreshWorker
                    >(
                24L,
                TimeUnit.HOURS,
                6L,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    20L,
                    TimeUnit.MINUTES
                )
                .setConstraints(networkConstraint)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    20L,
                    TimeUnit.MINUTES
                )
                .build()

        WorkManager.getInstance(
            context.applicationContext
        ).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
    }

    fun scheduleContinuation(
        context: Context,
        delayMinutes: Long = 20L
    ) {
        val continuation =
            OneTimeWorkRequestBuilder<
                    DailyPriceRefreshWorker
                    >()
                .setInitialDelay(
                    delayMinutes.coerceAtLeast(10L),
                    TimeUnit.MINUTES
                )
                .setConstraints(networkConstraint)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    20L,
                    TimeUnit.MINUTES
                )
                .build()

        WorkManager.getInstance(
            context.applicationContext
        ).enqueueUniqueWork(
            CONTINUATION_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            continuation
        )
    }
}