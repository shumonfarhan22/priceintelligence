import SwiftUI
import UIKit
import UserNotifications
import BackgroundTasks
import Shared

final class PriceIntelligenceAppDelegate:
    NSObject,
    UIApplicationDelegate,
    UNUserNotificationCenterDelegate {

    private let dailyRefreshTaskIdentifier =
        "com.supreme.priceintelligence.daily-price-refresh"

    private var activeDailyRefresh:
        IosDailyPriceRefreshTask?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions:
            [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        registerDailyPriceRefresh()
        scheduleDailyPriceRefresh(after: 15 * 60)
        return true
    }

    func applicationDidEnterBackground(
        _ application: UIApplication
    ) {
        scheduleDailyPriceRefresh(after: 15 * 60)
    }

    private func registerDailyPriceRefresh() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier:
                dailyRefreshTaskIdentifier,
            using: nil
        ) { [weak self] task in
            guard
                let self,
                let processingTask =
                    task as? BGProcessingTask
            else {
                task.setTaskCompleted(success: false)
                return
            }

            self.runDailyPriceRefresh(
                processingTask
            )
        }
    }

    private func scheduleDailyPriceRefresh(
        after delay: TimeInterval
    ) {
        let request =
            BGProcessingTaskRequest(
                identifier:
                    dailyRefreshTaskIdentifier
            )

        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate =
            Date(
                timeIntervalSinceNow: delay
            )

        do {
            try BGTaskScheduler.shared.submit(
                request
            )
        } catch {
            // Foreground catch-up remains available.
        }
    }

    private func runDailyPriceRefresh(
        _ task: BGProcessingTask
    ) {
        scheduleDailyPriceRefresh(
            after: 30 * 60
        )

        let bridge =
            IosDailyPriceRefreshTask()

        activeDailyRefresh = bridge

        task.expirationHandler = {
            bridge.cancel()
        }

        bridge.start(
            onCompleted: { [weak self] in
                DispatchQueue.main.async {
                    task.setTaskCompleted(
                        success: true
                    )

                    self?.activeDailyRefresh = nil

                    self?.scheduleDailyPriceRefresh(
                        after: 20 * 60 * 60
                    )
                }
            },
            onNeedsMoreTime: { [weak self] in
                DispatchQueue.main.async {
                    task.setTaskCompleted(
                        success: true
                    )

                    self?.activeDailyRefresh = nil

                    self?.scheduleDailyPriceRefresh(
                        after: 30 * 60
                    )
                }
            },
            onFailed: { [weak self] in
                DispatchQueue.main.async {
                    task.setTaskCompleted(
                        success: false
                    )

                    self?.activeDailyRefresh = nil

                    self?.scheduleDailyPriceRefresh(
                        after: 60 * 60
                    )
                }
            }
        )
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler:
            @escaping (
                UNNotificationPresentationOptions
            ) -> Void
    ) {
        completionHandler([
            .banner,
            .list,
            .sound
        ])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response:
            UNNotificationResponse,
        withCompletionHandler completionHandler:
            @escaping () -> Void
    ) {
        defer {
            completionHandler()
        }

        let userInfo =
            response
                .notification
                .request
                .content
                .userInfo

        guard
            let productNumber =
                userInfo[
                    "price_change_product_id"
                ] as? NSNumber,
            let retailer =
                userInfo[
                    "price_change_retailer"
                ] as? String,
            let oldPriceNumber =
                userInfo[
                    "price_change_old_price"
                ] as? NSNumber,
            let newPriceNumber =
                userInfo[
                    "price_change_new_price"
                ] as? NSNumber,
            let direction =
                userInfo[
                    "price_change_direction"
                ] as? String,
            let detectedAtNumber =
                userInfo[
                    "price_change_detected_at"
                ] as? NSNumber
        else {
            return
        }

        PriceChangeNotificationNavigation.shared
            .openPriceMovement(
                productId:
                    productNumber.int64Value,
                retailerName:
                    retailer,
                oldPrice:
                    oldPriceNumber.doubleValue,
                newPrice:
                    newPriceNumber.doubleValue,
                directionName:
                    direction,
                detectedAt:
                    detectedAtNumber.int64Value
            )
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(
        PriceIntelligenceAppDelegate.self
    )
    private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}