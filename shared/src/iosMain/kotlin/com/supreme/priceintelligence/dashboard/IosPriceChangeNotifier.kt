@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.supreme.priceintelligence.dashboard

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

class IosPriceChangeNotifier : PriceChangeNotifier {

    private val notificationCenter =
        UNUserNotificationCenter
            .currentNotificationCenter()

    override fun requestPermission() {
        notificationCenter
            .requestAuthorizationWithOptions(
                options =
                    UNAuthorizationOptionAlert or
                            UNAuthorizationOptionSound,
                completionHandler = { _, _ -> }
            )
    }

    override fun publishPriceChanges(
        changes: List<DetectedPriceChange>
    ) {
        if (changes.isEmpty()) {
            return
        }

        val message =
            buildPriceChangeNotificationText(changes)

        val content =
            UNMutableNotificationContent().apply {
                setTitle(message.title)
                setBody(message.body)
                setSound(
                    UNNotificationSound.defaultSound
                )
            }

        val request =
            UNNotificationRequest
                .requestWithIdentifier(
                    identifier =
                        PRICE_CHANGE_NOTIFICATION_ID,
                    content = content,
                    trigger = null
                )

        notificationCenter
            .removePendingNotificationRequestsWithIdentifiers(
                listOf(PRICE_CHANGE_NOTIFICATION_ID)
            )

        notificationCenter
            .removeDeliveredNotificationsWithIdentifiers(
                listOf(PRICE_CHANGE_NOTIFICATION_ID)
            )

        notificationCenter
            .addNotificationRequest(
                request = request,
                withCompletionHandler = { _ -> }
            )
    }

    private companion object {
        const val PRICE_CHANGE_NOTIFICATION_ID =
            "online-price-changes"
    }
}