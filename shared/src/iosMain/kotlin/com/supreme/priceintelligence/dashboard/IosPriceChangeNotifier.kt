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

        val content =
            UNMutableNotificationContent().apply {
                setTitle(message.title)
                setBody(message.body)
                setSound(
                    UNNotificationSound.defaultSound
                )

                setThreadIdentifier(
                    PRICE_CHANGE_NOTIFICATION_THREAD
                )

                setUserInfo(
                    mapOf(
                        "price_change_product_id" to
                            change.productId,
                        "price_change_retailer" to
                            change.retailer.name,
                        "price_change_old_price" to
                            change.oldPrice,
                        "price_change_new_price" to
                            change.newPrice,
                        "price_change_direction" to
                            change.direction.name,
                        "price_change_detected_at" to
                            change.detectedAt
                    )
                )
            }

        val identifier =
            "$PRICE_CHANGE_NOTIFICATION_PREFIX-" +
                "${change.productId}-" +
                change.retailer.name

        val request =
            UNNotificationRequest
                .requestWithIdentifier(
                    identifier = identifier,
                    content = content,
                    trigger = null
                )

        notificationCenter
            .removePendingNotificationRequestsWithIdentifiers(
                listOf(identifier)
            )

        notificationCenter
            .removeDeliveredNotificationsWithIdentifiers(
                listOf(identifier)
            )

        notificationCenter
            .addNotificationRequest(
                request = request,
                withCompletionHandler = { _ -> }
            )
    }

    private companion object {
        const val PRICE_CHANGE_NOTIFICATION_PREFIX =
            "online-price-change"

        const val PRICE_CHANGE_NOTIFICATION_THREAD =
            "online-price-changes"
    }
}