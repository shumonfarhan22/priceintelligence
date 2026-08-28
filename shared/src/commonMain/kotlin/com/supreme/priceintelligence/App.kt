package com.supreme.priceintelligence

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room3.RoomDatabase
import com.supreme.priceintelligence.dashboard.PricingInsightsScreen
import com.supreme.priceintelligence.dashboard.DashboardViewModel
import com.supreme.priceintelligence.dashboard.HubPriceMovementScreen
import com.supreme.priceintelligence.dashboard.QuickCompareScreen
import com.supreme.priceintelligence.dashboard.PriceChangeNotifier
import com.supreme.priceintelligence.dashboard.PriceChangeNotificationNavigation
import com.supreme.priceintelligence.dashboard.buildDecisionSummary
import com.supreme.priceintelligence.dashboard.buildPriceFreshnessSummary
import com.supreme.priceintelligence.data.AppDatabase
import com.supreme.priceintelligence.home.LaunchHubScreen
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.inventory.OriginalInventoryScreen
import com.supreme.priceintelligence.inventory.OriginalInventoryUndoBanner
import com.supreme.priceintelligence.inventory.InventoryViewModel
import com.supreme.priceintelligence.inventory.AppToolsDialog
import com.supreme.priceintelligence.inventory.PersonalizationAccordionDialog
import com.supreme.priceintelligence.inventory.rememberInventoryBackupActions
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.ui.components.OriginalAppBackground
import com.supreme.priceintelligence.ui.components.OriginalBannerKind
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

private enum class MainDestination {
    Dashboard,
    Inventory,
    PriceMovement,
    QuickCompare
}

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

            val hubFreshnessSummary = remember(
                dashboardState.allMatchingItems
            ) {
                dashboardState
                    .allMatchingItems
                    .buildPriceFreshnessSummary(
                        nowMillis =
                            kotlin.time.Clock.System
                                .now()
                                .toEpochMilliseconds()
                    )
            }

            val hubDecisionSummary = remember(
                dashboardState.allMatchingItems,
                dashboardState.pageItems
            ) {
                dashboardState
                    .allMatchingItems
                    .buildDecisionSummary(
                        dashboardState.pageItems
                    )
            }

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

            LaunchedEffect(
                customization.automaticPriceChecksEnabled
            ) {
                dashboardViewModel
                    .applyAutomaticPriceCheckPreference(
                        customization
                            .automaticPriceChecksEnabled
                    )
            }

            var destinationName by rememberSaveable {
                mutableStateOf(
                    MainDestination.Dashboard.name
                )
            }

            var hubVisible by rememberSaveable {
                mutableStateOf(true)
            }

            var personalizationOpen by rememberSaveable {
                mutableStateOf(false)
            }

            var appToolsOpen by rememberSaveable {
                mutableStateOf(false)
            }

            val backupActions =
                rememberInventoryBackupActions(
                    viewModel = inventoryViewModel,
                    onImportCompleted = {
                        appToolsOpen = false
                        personalizationOpen = false
                        hubVisible = true
                    }
                )

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
                MainDestination.entries
                    .firstOrNull { item ->
                        item.name == destinationName
                    }
                    ?: MainDestination.Dashboard

            LaunchedEffect(
                priceMovementNotificationTarget
                    ?.requestId
            ) {
                if (
                    priceMovementNotificationTarget !=
                    null
                ) {
                    destinationName =
                        MainDestination
                            .PriceMovement
                            .name
                    hubVisible = false
                }
            }

            LaunchedEffect(
                destination,
                hubVisible
            ) {
                dashboardViewModel.setAutomaticRefreshPaused(
                    reason = "main-destination",
                    paused =
                        !hubVisible &&
                            destination !=
                            MainDestination.Dashboard
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
                12.dp,
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

            val navigateBack: () -> Unit = {
                when {
                    destination ==
                        MainDestination.Inventory &&
                        inventoryState.isSelectionMode -> {
                        inventoryViewModel.clearSelection()
                    }

                    else -> {
                        priceMovementNotificationTarget
                            ?.requestId
                            ?.let(
                                PriceChangeNotificationNavigation::
                                    consume
                            )

                        hubVisible = true
                    }
                }
            }

            PlatformBackHandler(
                enabled = !hubVisible,
                onBack = navigateBack
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
                        .platformBackSwipe(
                            enabled = !hubVisible,
                            onBack = navigateBack
                        )
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
                                targetState =
                                    if (hubVisible) {
                                        null
                                    } else {
                                        destination
                                    },
                                transitionSpec = {
                                    when {
                                        reduceMotionEnabled -> {
                                            EnterTransition.None togetherWith
                                                ExitTransition.None
                                        }

                                        initialState == null &&
                                            targetState != null -> {
                                            (
                                                fadeIn(
                                                    animationSpec = tween(
                                                        durationMillis = 180,
                                                        delayMillis = 25,
                                                        easing =
                                                            FastOutSlowInEasing
                                                    )
                                                ) +
                                                    scaleIn(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.88f,
                                                            stiffness = 440f
                                                        ),
                                                        initialScale = 0.93f
                                                    ) +
                                                    slideInVertically(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.90f,
                                                            stiffness = 500f
                                                        )
                                                    ) { height ->
                                                        height / 16
                                                    }
                                            ) togetherWith (
                                                fadeOut(
                                                    animationSpec = tween(
                                                        durationMillis = 135,
                                                        easing =
                                                            FastOutLinearInEasing
                                                    )
                                                ) +
                                                    scaleOut(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.92f,
                                                            stiffness = 520f
                                                        ),
                                                        targetScale = 0.97f
                                                    ) +
                                                    slideOutVertically(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.94f,
                                                            stiffness = 560f
                                                        )
                                                    ) { height ->
                                                        -height / 40
                                                    }
                                            )
                                        }

                                        targetState == null -> {
                                            (
                                                fadeIn(
                                                    animationSpec = tween(
                                                        durationMillis = 190,
                                                        delayMillis = 20,
                                                        easing =
                                                            FastOutSlowInEasing
                                                    )
                                                ) +
                                                    scaleIn(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.92f,
                                                            stiffness = 520f
                                                        ),
                                                        initialScale = 0.97f
                                                    ) +
                                                    slideInVertically(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.94f,
                                                            stiffness = 560f
                                                        )
                                                    ) { height ->
                                                        -height / 40
                                                    }
                                            ) togetherWith (
                                                fadeOut(
                                                    animationSpec = tween(
                                                        durationMillis = 140,
                                                        easing =
                                                            FastOutLinearInEasing
                                                    )
                                                ) +
                                                    scaleOut(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.90f,
                                                            stiffness = 500f
                                                        ),
                                                        targetScale = 1.04f
                                                    ) +
                                                    slideOutVertically(
                                                        animationSpec = spring(
                                                            dampingRatio = 0.90f,
                                                            stiffness = 500f
                                                        )
                                                    ) { height ->
                                                        height / 16
                                                    }
                                            )
                                        }

                                        else -> {
                                            fadeIn(
                                                animationSpec = tween(
                                                    durationMillis = 180,
                                                    easing =
                                                        FastOutSlowInEasing
                                                )
                                            ) togetherWith fadeOut(
                                                animationSpec = tween(
                                                    durationMillis = 140,
                                                    easing =
                                                        FastOutLinearInEasing
                                                )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                label = "mainDestinationTransition"
                            ) { visibleDestination ->
                                when (visibleDestination) {
                                    null ->
                                        LaunchHubScreen(
                                            isConnected =
                                                isConnected,
                                            decisionSummary =
                                                hubDecisionSummary,
                                            freshnessSummary =
                                                hubFreshnessSummary,
                                            activeFilter =
                                                dashboardState
                                                    .priceFilter,
                                            refreshTick =
                                                dashboardState
                                                    .refreshCollapseTick,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            insightCustomization =
                                                customization
                                                    .insightCustomization,
                                            onDashboardClick = {
                                                destinationName =
                                                    MainDestination
                                                        .Dashboard
                                                        .name
                                                hubVisible = false
                                                dashboardViewModel
                                                    .refreshSilently()
                                            },
                                            onInventoryClick = {
                                                destinationName =
                                                    MainDestination
                                                        .Inventory
                                                        .name
                                                hubVisible = false
                                            },
                                            onPriceMovementClick = {
                                                destinationName =
                                                    MainDestination
                                                        .PriceMovement
                                                        .name
                                                hubVisible = false
                                            },
                                            onQuickCompareClick = {
                                                destinationName =
                                                    MainDestination
                                                        .QuickCompare
                                                        .name
                                                hubVisible = false
                                            },
                                            onSettingsClick = {
                                                appToolsOpen = true
                                            },
                                            onFilterSelected = {
                                                    filter ->
                                                dashboardViewModel
                                                    .setPriceFilter(
                                                        filter
                                                    )
                                                destinationName =
                                                    MainDestination
                                                        .Dashboard
                                                        .name
                                                hubVisible = false
                                            },
                                            modifier =
                                                Modifier.fillMaxSize()
                                        )

                                    MainDestination.Dashboard ->
                                        PricingInsightsScreen(
                                            viewModel = dashboardViewModel,
                                            insightCustomization =
                                                customization
                                                    .insightCustomization,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            modifier = Modifier.fillMaxSize(),
                                            onNavigateHome = {
                                                hubVisible = true
                                            }
                                        )

                                    MainDestination.Inventory ->
                                        OriginalInventoryScreen(
                                            viewModel = inventoryViewModel,
                                            customization =
                                                customization,
                                            onNavigateHome = {
                                                hubVisible = true
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

                                    MainDestination.PriceMovement ->
                                        HubPriceMovementScreen(
                                            viewModel =
                                                dashboardViewModel,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            customization =
                                                customization,
                                            notificationTarget =
                                                priceMovementNotificationTarget,
                                            onNotificationConsumed =
                                                PriceChangeNotificationNavigation::
                                                    consume,
                                            onNavigateHome = {
                                                hubVisible = true
                                            },
                                            modifier =
                                                Modifier.fillMaxSize()
                                        )

                                    MainDestination.QuickCompare ->
                                        QuickCompareScreen(
                                            viewModel =
                                                dashboardViewModel,
                                            advancedModeEnabled =
                                                advancedModeEnabled,
                                            reduceMotionEnabled =
                                                reduceMotionEnabled,
                                            customization =
                                                customization,
                                            onNavigateHome = {
                                                hubVisible = true
                                            },
                                            modifier =
                                                Modifier.fillMaxSize()
                                        )
                                }
                            }
                        }
                    }

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

                    // The launch hub now provides all main navigation.
                }
            }

            if (appToolsOpen) {
                AppToolsDialog(
                    onImportBackup = {
                        appToolsOpen = false
                        backupActions.importBackup()
                    },
                    onExportBackup = {
                        appToolsOpen = false
                        backupActions.exportBackup()
                    },
                    onPersonalize = {
                        appToolsOpen = false
                        personalizationOpen = true
                    },
                    onDismiss = {
                        appToolsOpen = false
                    }
                )
            }

            if (personalizationOpen) {
                PersonalizationAccordionDialog(
                    dashboardViewModel =
                        dashboardViewModel,
                    themeMode = themeMode,
                    customization = customization,
                    advancedModeEnabled =
                        advancedModeEnabled,
                    priceChangeNotificationsEnabled =
                        priceChangeNotificationsEnabled,
                    reduceMotionEnabled =
                        reduceMotionEnabled,
                    onThemeModeChanged = { selectedMode ->
                        themeMode = selectedMode
                        appPreferences.themeMode =
                            selectedMode
                    },
                    onCustomizationChanged = { updated ->
                        customization = updated
                        appPreferences.customizationProfile =
                            writeAppCustomization(updated)
                    },
                    onAdvancedModeChanged = { enabled ->
                        advancedModeEnabled = enabled
                        appPreferences.advancedModeEnabled =
                            enabled
                    },
                    onPriceChangeNotificationsChanged = {
                            enabled ->
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
                    onResetPersonalization = {
                        themeMode = AppThemeMode.DARK
                        appPreferences.themeMode =
                            AppThemeMode.DARK

                        val resetCustomization =
                            AppCustomization(
                                savedColorPreset =
                                    customization
                                        .savedColorPreset,
                                savedPersonalizationPreset =
                                    customization
                                        .savedPersonalizationPreset,
                                savedPersonalizationPresets =
                                    customization
                                        .savedPersonalizationPresets
                            )

                        customization = resetCustomization
                        appPreferences.customizationProfile =
                            writeAppCustomization(
                                resetCustomization
                            )
                    },
                    onDismiss = {
                        personalizationOpen = false
                    }
                )
            }
        }
    }
}
