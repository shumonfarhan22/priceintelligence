package com.supreme.priceintelligence

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.supreme.priceintelligence.dashboard.OriginalDashboardScreen
import com.supreme.priceintelligence.dashboard.DashboardViewModel
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
import com.supreme.priceintelligence.ui.theme.PriceIntelligenceTheme
import com.supreme.priceintelligence.settings.AppPreferences
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun App(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    networkMonitor: NetworkMonitor,
    appPreferences: AppPreferences
) {
    PriceIntelligenceTheme {
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
                    networkMonitor = networkMonitor
                )
            }
            val inventoryState by inventoryViewModel.uiState.collectAsState()
            val dashboardState by dashboardViewModel.uiState.collectAsState()
            var destinationName by rememberSaveable {
                mutableStateOf(AppDestination.Dashboard.name)
            }
            var advancedModeEnabled by remember {
                mutableStateOf(appPreferences.advancedModeEnabled)
            }
            val destination = AppDestination.entries.firstOrNull { item ->
                item.name == destinationName
            } ?: AppDestination.Dashboard

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
                enabled = destination != AppDestination.Dashboard,
                onBack = {
                    if (
                        destination == AppDestination.Inventory &&
                        inventoryState.isSelectionMode
                    ) {
                        inventoryViewModel.clearSelection()
                    } else {
                        destinationName = AppDestination.Dashboard.name
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
                            when (destination) {
                                AppDestination.Dashboard -> OriginalDashboardScreen(
                                    viewModel = dashboardViewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    advancedModeEnabled = advancedModeEnabled,
                                    bottomBannerHeight = bottomBannerHeight
                                )

                                AppDestination.Inventory -> OriginalInventoryScreen(
                                    viewModel = inventoryViewModel,
                                    advancedModeEnabled = advancedModeEnabled,
                                    onAdvancedModeChanged = { enabled ->
                                        advancedModeEnabled = enabled
                                        appPreferences.advancedModeEnabled = enabled
                                    },
                                    bottomBannerHeight = bottomBannerHeight,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                )
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
                        horizontalPadding = 16.dp
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
                        horizontalPadding = 16.dp
                    )

                    OriginalBottomNavigation(
                        selectedDestination = destination,
                        modifier = Modifier.align(
                            androidx.compose.ui.Alignment.BottomCenter
                        ),
                        horizontalPadding = 16.dp,
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
