package com.supreme.priceintelligence.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AndroidPriceChangeNotifier(
    context: Context,
    private val permissionActivity:
        ComponentActivity? = null
) : PriceChangeNotifier {

    constructor(activity: ComponentActivity) : this(
        context = activity.applicationContext,
        permissionActivity = activity
    )

    private val appContext =
        context.applicationContext

    override fun requestPermission() {
        createNotificationChannel()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionActivity?.let { activity ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun publishPriceChanges(
        changes: List<DetectedPriceChange>
    ) {
        if (
            changes.isEmpty() ||
            !canPostNotifications()
        ) {
            return
        }

        createNotificationChannel()

        changes
            .distinctBy { change ->
                Triple(
                    change.productId,
                    change.retailer,
                    change.detectedAt
                )
            }
            .forEach { change ->
                publishPriceChange(change)
            }
    }

    private fun publishPriceChange(
        change: DetectedPriceChange
    ) {
        val message =
            buildPriceChangeNotificationText(
                listOf(change)
            )

        val notificationBuilder =
            NotificationCompat.Builder(
                appContext,
                PRICE_CHANGE_CHANNEL_ID
            )
                .setSmallIcon(appContext.applicationInfo.icon)
                .setContentTitle(message.title)
                .setContentText(message.body)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message.body)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setGroup(
                    PRICE_CHANGE_NOTIFICATION_GROUP
                )

        createLaunchPendingIntent(change)
            ?.let { pendingIntent ->
                notificationBuilder.setContentIntent(
                    pendingIntent
                )
            }

        NotificationManagerCompat
            .from(appContext)
            .notify(
                notificationId(change),
                notificationBuilder.build()
            )
    }

    private fun createLaunchPendingIntent(
        change: DetectedPriceChange
    ): PendingIntent? {
        val launchIntent =
            appContext.packageManager
                .getLaunchIntentForPackage(
                    appContext.packageName
                )
                ?: return null

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_PRODUCT_ID,
            change.productId
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_RETAILER,
            change.retailer.name
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_OLD_PRICE,
            change.oldPrice
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_NEW_PRICE,
            change.newPrice
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_DIRECTION,
            change.direction.name
        )

        launchIntent.putExtra(
            EXTRA_PRICE_CHANGE_DETECTED_AT,
            change.detectedAt
        )

        return PendingIntent.getActivity(
            appContext,
            notificationId(change),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationId(
        change: DetectedPriceChange
    ): Int {
        val productPart =
            (
                change.productId xor
                    (change.productId ushr 32)
            ).toInt()

        return (
            productPart * 31 +
                change.retailer.ordinal +
                PRICE_CHANGE_NOTIFICATION_ID_BASE
            ) and Int.MAX_VALUE
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted =
            Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        return permissionGranted &&
                NotificationManagerCompat
                    .from(appContext)
                    .areNotificationsEnabled()
    }

    private fun createNotificationChannel() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val notificationManager =
            appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val channel = NotificationChannel(
            PRICE_CHANGE_CHANNEL_ID,
            "Online price changes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                "Changes detected during the daily Amazon and Flipkart check"
        }

        notificationManager.createNotificationChannel(
            channel
        )
    }

    private companion object {
        const val PRICE_CHANGE_CHANNEL_ID =
            "online_price_changes"

        const val PRICE_CHANGE_NOTIFICATION_ID_BASE =
            2401

        const val PRICE_CHANGE_NOTIFICATION_GROUP =
            "online-price-changes"

        const val NOTIFICATION_PERMISSION_REQUEST_CODE =
            2402
    }
}

fun handlePriceChangeNotificationIntent(
    intent: Intent?
) {
    intent ?: return

    val productId = intent.getLongExtra(
        EXTRA_PRICE_CHANGE_PRODUCT_ID,
        0L
    )

    if (productId <= 0L) {
        return
    }

    PriceChangeNotificationNavigation.openPriceMovement(
        productId = productId,
        retailerName =
            intent.getStringExtra(
                EXTRA_PRICE_CHANGE_RETAILER
            ).orEmpty(),
        oldPrice = intent.getDoubleExtra(
            EXTRA_PRICE_CHANGE_OLD_PRICE,
            0.0
        ),
        newPrice = intent.getDoubleExtra(
            EXTRA_PRICE_CHANGE_NEW_PRICE,
            0.0
        ),
        directionName =
            intent.getStringExtra(
                EXTRA_PRICE_CHANGE_DIRECTION
            ).orEmpty(),
        detectedAt = intent.getLongExtra(
            EXTRA_PRICE_CHANGE_DETECTED_AT,
            0L
        )
    )

    intent.removeExtra(
        EXTRA_PRICE_CHANGE_PRODUCT_ID
    )
}

private const val EXTRA_PRICE_CHANGE_PRODUCT_ID =
    "price_change_product_id"

private const val EXTRA_PRICE_CHANGE_RETAILER =
    "price_change_retailer"

private const val EXTRA_PRICE_CHANGE_OLD_PRICE =
    "price_change_old_price"

private const val EXTRA_PRICE_CHANGE_NEW_PRICE =
    "price_change_new_price"

private const val EXTRA_PRICE_CHANGE_DIRECTION =
    "price_change_direction"

private const val EXTRA_PRICE_CHANGE_DETECTED_AT =
    "price_change_detected_at"