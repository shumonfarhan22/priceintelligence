package com.supreme.priceintelligence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room3.RoomDatabase
import com.supreme.priceintelligence.dashboard.DashboardScreen
import com.supreme.priceintelligence.dashboard.DashboardViewModel
import com.supreme.priceintelligence.data.AppDatabase
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.inventory.InventoryScreen
import com.supreme.priceintelligence.inventory.InventoryUndoBanner
import com.supreme.priceintelligence.inventory.InventoryViewModel
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.ui.components.AppDestination
import com.supreme.priceintelligence.ui.components.SupremeAmbientBackground
import com.supreme.priceintelligence.ui.components.SupremeBottomNavigation
import com.supreme.priceintelligence.ui.components.SupremeHeader
import com.supreme.priceintelligence.ui.theme.PriceIntelligenceTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun App(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    networkMonitor: NetworkMonitor
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
            var destinationName by rememberSaveable {
                mutableStateOf(AppDestination.Dashboard.name)
            }
            val destination = AppDestination.entries.firstOrNull { item ->
                item.name == destinationName
            } ?: AppDestination.Dashboard

            LaunchedEffect(inventoryState.pendingDeletes) {
                if (inventoryState.pendingDeletes.isNotEmpty()) {
                    delay(4000.milliseconds)
                    if (inventoryViewModel.uiState.value.pendingDeletes.isNotEmpty()) {
                        inventoryViewModel.commitDelete()
                    }
                }
            }

            SupremeAmbientBackground {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                ) {
                    val horizontalPadding = when {
                        maxWidth < 380.dp -> 14.dp
                        maxWidth < 410.dp -> 18.dp
                        else -> 20.dp
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SupremeHeader(
                            isConnected = isConnected,
                            horizontalPadding = horizontalPadding
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding)
                        ) {
                            when (destination) {
                                AppDestination.Dashboard -> DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )

                                AppDestination.Inventory -> InventoryScreen(
                                    viewModel = inventoryViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        InventoryUndoBanner(
                            pendingItems = inventoryState.pendingDeletes,
                            onUndo = inventoryViewModel::cancelDelete,
                            horizontalPadding = horizontalPadding
                        )

                        SupremeBottomNavigation(
                            selectedDestination = destination,
                            horizontalPadding = horizontalPadding,
                            onDestinationSelected = { selected ->
                                destinationName = selected.name
                                if (selected == AppDestination.Dashboard) {
                                    dashboardViewModel.refresh()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
