package com.supreme.priceintelligence

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room3.RoomDatabase
import com.supreme.priceintelligence.dashboard.OriginalDashboardScreen
import com.supreme.priceintelligence.dashboard.DashboardViewModel
import com.supreme.priceintelligence.dashboard.PriceChangeNotifier
import com.supreme.priceintelligence.dashboard.PriceChangeNotificationNavigation
import com.supreme.priceintelligence.data.AppDatabase
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.inventory.OriginalInventoryScreen
import com.supreme.priceintelligence.inventory.OriginalInventoryUndoBanner
import com.supreme.priceintelligence.inventory.InventoryViewModel
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.ui.components.AppDestination
import com.supreme.priceintelligence.ui.components.OriginalAppBackground
import com.supreme.priceintelligence.ui.components.OriginalBannerKind
import com.supreme.priceintelligence.ui.components.OriginalBottomNavigation
import com.supreme.priceintelligence.ui.components.OriginalDashboardHeader
import com.supreme.priceintelligence.ui.components.OriginalStatusBanner
import com.supreme.priceintelligence.settings.AppPreferences
import com.supreme.priceintelligence.settings.AppCustomization
import com.supreme.priceintelligence.settings.AppDisplayDensity
import com.supreme.priceintelligence.settings.AppMotionPreference
import com.supreme.priceintelligence.settings.AppThemeMode
import com.supreme.priceintelligence.settings.readAppCustomization
import com.supreme.priceintelligence.settings.writeAppCustomization
import com.supreme.priceintelligence.ui.theme.PriceIntelligenceTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun App(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    networkMonitor: NetworkMonitor,
    appPreferences: AppPreferences,
    priceChangeNotifier: PriceChangeNotifier,
    onThemeApplied: (
        themeMode: AppThemeMode,
        isDarkTheme: Boolean
    ) -> Unit = { _, _ -> }
) {
    var themeMode by remember {
        mutableStateOf(appPreferences.themeMode)
    }

    var customization by remember {
        mutableStateOf(
            readAppCustomization(
                appPreferences.customizationProfile
            )
        )
    }

    PriceIntelligenceTheme(
        themeMode = themeMode,
        customization = customization
    ) { isDarkTheme ->
        LaunchedEffect(themeMode, isDarkTheme) {
            onThemeApplied(themeMode, isDarkTheme)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val repository = remember(databaseBuilder) {
                InventoryRepository(getRoomDatabase(databaseBuilder).inventoryDao())
            }
            val isConnected by networkMonitor.isConnected.collectAsState()
            val inventoryViewModel: InventoryViewModel = viewModel {
                InventoryViewModel(repository)
            }
            val priceScraper = remember { PriceScraper() }

            DisposableEffect(priceScraper) {
                onDispose(priceScraper::close)
            }

            val dashboardViewModel: DashboardViewModel = viewModel {
                DashboardViewModel(
                    repository = repository,
                    scraper = priceScraper,
                    networkMonitor = networkMonitor,
                    appPreferences = appPreferences,
                    priceChangeNotifier =
                        priceChangeNotifier
                )
            }
            val inventoryState by inventoryViewModel.uiState.collectAsState()
            val dashboardState by dashboardViewModel.uiState.collectAsState()

            val priceMovementNotificationTarget by
                PriceChangeNotificationNavigation
                    .pendingTarget
                    .collectAsState()

            val systemReduceMotionEnabled =
                rememberReduceMotionEnabled()

            val reduceMotionEnabled =
                systemReduceMotionEnabled ||
                    customization.motionPreference ==
                    AppMotionPreference.REDUCED

            LaunchedEffect(
                customization.dashboardDefaultSort,
                customization.dashboardPageSize
            ) {
                dashboardViewModel
                    .applyDashboardPreferences(
                        defaultSortOrderName =
                            customization
                                .dashboardDefaultSort
                                .name,
                        pageSize =
                            customization
                                .dashboardPageSize
                                .productCount
                    )
            }

            var destinationName by rememberSaveable {
                mutableStateOf(AppDestination.Dashboard.name)
            }
            var advancedModeEnabled by remember {
                mutableStateOf(appPreferences.advancedModeEnabled)
            }
            var priceChangeNotificationsEnabled by remember {
                mutableStateOf(
                    appPreferences
                        .priceChangeNotificationsEnabled
                )
            }
            val destination =
                AppDestination.entries.firstOrNull { item ->
                    item.name == destinationName
                } ?: AppDestination.Dashboard

            LaunchedEffect(
                priceMovementNotificationTarget
                    ?.requestId
            ) {
                if (
                    priceMovementNotificationTarget !=
                    null
                ) {
                    destinationName =
                        AppDestination.Dashboard.name
                }
            }

            LaunchedEffect(destination) {
                dashboardViewModel.setAutomaticRefreshPaused(
                    reason = "main-destination",
                    paused =
                        destination !=
                            AppDestination.Dashboard
                )
            }

            val density = LocalDensity.current

            val keyboardBottom =
                WindowInsets.ime
                    .asPaddingValues()
                    .calculateBottomPadding()

            val systemNavigationBottom =
                WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()

            val keyboardClearance = (
                keyboardBottom - systemNavigationBottom
            ).coerceAtLeast(0.dp)

            val bannerBaseBottom = maxOf(
                96.dp,
                keyboardClearance + 8.dp
            )

            val statusBannerVisible =
                !inventoryState.statusMessage.isNullOrBlank()
            val undoBannerVisible =
                inventoryState.pendingDeletes.isNotEmpty()

            var measuredStatusBannerHeight by remember {
                mutableStateOf(0.dp)
            }
            var measuredUndoBannerHeight by remember {
                mutableStateOf(0.dp)
            }

            val requestedBottomBannerHeight = when {
                statusBannerVisible && undoBannerVisible ->
                    measuredUndoBannerHeight +
                        8.dp +
                        measuredStatusBannerHeight

                statusBannerVisible ->
                    measuredStatusBannerHeight

                undoBannerVisible ->
                    measuredUndoBannerHeight

                else ->
                    0.dp
            }

            val bottomBannerHeight by animateDpAsState(
                targetValue = requestedBottomBannerHeight,
                animationSpec = tween(durationMillis = 180),
                label = "bottomBannerClearance"
            )

            val statusBannerStackOffset by animateDpAsState(
                targetValue = if (undoBannerVisible) {
                    measuredUndoBannerHeight + 8.dp
                } else {
                    0.dp
                },
                animationSpec = tween(durationMillis = 180),
                label = "statusBannerPosition"
            )

            PlatformBackHandler(
                enabled =
                    destination != AppDestination.Dashboard ||
                        dashboardState.currentPage > 1,
                onBack = {
                    when {
                        destination ==
                            AppDestination.Dashboard &&
                            dashboardState.currentPage > 1 -> {
                            dashboardViewModel.goToPage(
                                dashboardState.currentPage - 1
                            )
                        }

                        destination ==
                            AppDestination.Inventory &&
                            inventoryState.isSelectionMode -> {
                            inventoryViewModel.clearSelection()
                        }

                        else -> {
                            destinationName =
                                AppDestination.Dashboard.name
                        }
                    }
                }
            )

            LaunchedEffect(inventoryState.pendingDeletes) {
                if (inventoryState.pendingDeletes.isNotEmpty()) {
                    delay(4000.milliseconds)
                    if (inventoryViewModel.uiState.value.pendingDeletes.isNotEmpty()) {
                        inventoryViewModel.commitDelete()
                    }
                }
            }

            OriginalAppBackground(
                isConnected = isConnected,
                filterBloom = dashboardState.priceFilter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = destination,
                                transitionSpec = {
                                    if (reduceMotionEnabled) {
                                        EnterTransition.None togetherWith
                                                ExitTransition.None
                                    } else if (
                                        targetState.ordinal >
                                        initialState.ordinal
                                    ) {
                                        (
                                            slideInHorizontally(
                                                animationSpec = tween(
                                                    durationMillis = 220
                                                )
                                            ) { width ->
                                                width / 5
                                            } +
                                                fadeIn(
                                                    animationSpec = tween(
                                                        durationMillis = 160
                                                    )
                                                )
                                        ) togetherWith (
                                            slideOutHorizontally(
                                                animationSpec = tween(
                                                    durationMillis = 180
                                                )
                                            ) { width ->
                                                -width / 5
                                            } +
                                                fadeOut(
                                                    animationSpec = tween(
                                                        durationMillis = 140
                                                    )
                                                )
                                        )
                                    } else {
                                        (
                                            slideInHorizontally(
                                                animationSpec = tween(
                                                    durationMillis = 220
                                                )
                                            ) { width ->
                                                -width / 5
                                            } +
                                                fadeIn(
                                                    animationSpec = tween(
                                                        durationMillis = 160
                                                    )
                                                )
                                        ) togetherWith (
                                            slideOutHorizontally(
                                                animationSpec = tween(
                                                    durationMillis = 180
                                                )
                                            ) { width ->
                                                width / 5
                                            } +
                                                fadeOut(
                                                    animationSpec = tween(
                                                        durationMillis = 140
                                                    )
                                                )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "mainDestinationTransition"
                            ) { visibleDestination ->
                                when (visibleDestination) {
                                    AppDestination.Dashboard ->
                                        OriginalDashboardScreen(
                                            viewModel = dashboardViewModel,
                                            modifier = Modifier.fillMaxSize(),
                                            advancedModeEnabled =
                                                advancedModeEnabled,
                                            bottomBannerHeight =
                                                bottomBannerHeight,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            hapticsEnabled =
                                                customization
                                                    .hapticsEnabled,
                                            dashboardCardStyle =
                                                customization
                                                    .dashboardCardStyle,
                                            displayDensity =
                                                customization
                                                    .displayDensity,
                                            defaultPriceMovementRange =
                                                customization
                                                    .priceMovementDefaultRange,
                                            priceMovementNotificationTarget =
                                                priceMovementNotificationTarget,
                                            onPriceMovementNotificationConsumed =
                                                PriceChangeNotificationNavigation::consume
                                        )

                                    AppDestination.Inventory ->
                                        OriginalInventoryScreen(
                                            viewModel = inventoryViewModel,
                                            themeMode = themeMode,
                                            onThemeModeChanged = { selectedMode ->
                                                themeMode = selectedMode
                                                appPreferences.themeMode =
                                                    selectedMode
                                            },
                                            customization =
                                                customization,
                                            onCustomizationChanged =
                                                { updated ->
                                                    customization =
                                                        updated
                                                    appPreferences
                                                        .customizationProfile =
                                                        writeAppCustomization(
                                                            updated
                                                        )
                                                },
                                            onResetPersonalization = {
                                                themeMode =
                                                    AppThemeMode.DARK
                                                appPreferences.themeMode =
                                                    AppThemeMode.DARK
                                                customization =
                                                    AppCustomization()
                                                appPreferences
                                                    .customizationProfile =
                                                    writeAppCustomization(
                                                        AppCustomization()
                                                    )
                                            },
                                            advancedModeEnabled =
                                                advancedModeEnabled,
                                            onAdvancedModeChanged = { enabled ->
                                                advancedModeEnabled = enabled
                                                appPreferences
                                                    .advancedModeEnabled =
                                                    enabled
                                            },
                                            priceChangeNotificationsEnabled =
                                                priceChangeNotificationsEnabled,
                                            onPriceChangeNotificationsChanged =
                                                { enabled ->
                                                    priceChangeNotificationsEnabled =
                                                        enabled
                                                    appPreferences
                                                        .priceChangeNotificationsEnabled =
                                                        enabled

                                                    if (enabled) {
                                                        priceChangeNotifier
                                                            .requestPermission()
                                                    }
                                                },
                                            bottomBannerHeight =
                                                bottomBannerHeight,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    horizontal =
                                                        if (
                                                            customization
                                                                .displayDensity ==
                                                            AppDisplayDensity.COMPACT
                                                        ) {
                                                            12.dp
                                                        } else {
                                                            16.dp
                                                        }
                                                )
                                        )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(
                                androidx.compose.ui.Alignment.BottomCenter
                            )
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(
                                            alpha =
                                                if (isDarkTheme) {
                                                    0.12f
                                                } else {
                                                    0.04f
                                                }
                                        ),
                                        Color.Black.copy(
                                            alpha =
                                                if (isDarkTheme) {
                                                    0.48f
                                                } else {
                                                    0.14f
                                                }
                                        )
                                    )
                                )
                            )
                    )

                    OriginalStatusBanner(
                        message = inventoryState.statusMessage,
                        kind = when {
                            inventoryState.statusIsError ->
                                OriginalBannerKind.ERROR

                            inventoryState.statusIsInfo ->
                                OriginalBannerKind.INFO

                            else ->
                                OriginalBannerKind.SUCCESS
                        },
                        onDismiss = inventoryViewModel::clearStatus,
                        modifier = Modifier
                            .align(
                                androidx.compose.ui.Alignment.BottomCenter
                            )
                            .padding(
                                bottom =
                                    bannerBaseBottom +
                                        statusBannerStackOffset
                            )
                            .onSizeChanged { size ->
                                measuredStatusBannerHeight =
                                    with(density) {
                                        size.height.toDp()
                                    }
                            },
                        horizontalPadding =
                            if (
                                customization.displayDensity ==
                                AppDisplayDensity.COMPACT
                            ) {
                                12.dp
                            } else {
                                16.dp
                            }
                    )

                    OriginalInventoryUndoBanner(
                        pendingItems = inventoryState.pendingDeletes,
                        onUndo = inventoryViewModel::cancelDelete,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.BottomCenter)
                            .padding(bottom = bannerBaseBottom)
                            .onSizeChanged { size ->
                                measuredUndoBannerHeight =
                                    with(density) {
                                        size.height.toDp()
                                    }
                            },
                        horizontalPadding =
                            if (
                                customization.displayDensity ==
                                AppDisplayDensity.COMPACT
                            ) {
                                12.dp
                            } else {
                                16.dp
                            }
                    )

                    OriginalBottomNavigation(
                        selectedDestination = destination,
                        modifier = Modifier.align(
                            androidx.compose.ui.Alignment.BottomCenter
                        ),
                        horizontalPadding =
                            if (
                                customization.displayDensity ==
                                AppDisplayDensity.COMPACT
                            ) {
                                12.dp
                            } else {
                                16.dp
                            },
                        reduceMotionEnabled = reduceMotionEnabled,
                        onDestinationSelected = { selected ->
                            destinationName = selected.name

                            if (selected == AppDestination.Dashboard) {
                                dashboardViewModel.refreshSilently()
                            }
                        }
                    )
                }
            }
        }
    }
}
