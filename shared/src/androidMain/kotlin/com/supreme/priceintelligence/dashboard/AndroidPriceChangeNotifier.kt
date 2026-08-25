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
    private val activity: ComponentActivity
) : PriceChangeNotifier {

    override fun requestPermission() {
        createNotificationChannel()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
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

        val message =
            buildPriceChangeNotificationText(changes)

        val notificationBuilder =
            NotificationCompat.Builder(
                activity,
                PRICE_CHANGE_CHANNEL_ID
            )
                .setSmallIcon(activity.applicationInfo.icon)
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

        createLaunchPendingIntent()?.let { pendingIntent ->
            notificationBuilder.setContentIntent(
                pendingIntent
            )
        }

        NotificationManagerCompat
            .from(activity)
            .notify(
                PRICE_CHANGE_NOTIFICATION_ID,
                notificationBuilder.build()
            )
    }

    private fun createLaunchPendingIntent(): PendingIntent? {
        val launchIntent =
            activity.packageManager
                .getLaunchIntentForPackage(
                    activity.packageName
                )
                ?: return null

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

        return PendingIntent.getActivity(
            activity,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(): Boolean {
        val permissionGranted =
            Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        return permissionGranted &&
                NotificationManagerCompat
                    .from(activity)
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
            activity.getSystemService(
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

        const val PRICE_CHANGE_NOTIFICATION_ID =
            2401

        const val NOTIFICATION_PERMISSION_REQUEST_CODE =
            2402
    }
}