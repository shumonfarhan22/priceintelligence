package com.supreme.priceintelligence

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.app_logo
import com.supreme.priceintelligence.ui.theme.PriceIntelligenceTheme
import org.jetbrains.compose.resources.painterResource

private enum class AppDestination(
    val title: String,
    val shortLabel: String
) {
    Dashboard(
        title = "Dashboard",
        shortLabel = "D"
    ),
    Inventory(
        title = "Inventory",
        shortLabel = "I"
    )
}

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
                InventoryRepository(
                    getRoomDatabase(databaseBuilder).inventoryDao()
                )
            }

            val isConnected by networkMonitor.isConnected.collectAsState()

            val inventoryViewModel: InventoryViewModel = viewModel {
                InventoryViewModel(repository)
            }

            val dashboardViewModel: DashboardViewModel = viewModel {
                DashboardViewModel(
                    repository = repository,
                    scraper = PriceScraper(),
                    networkMonitor = networkMonitor
                )
            }

            val inventoryState by inventoryViewModel.uiState.collectAsState()

            var destination by remember {
                mutableStateOf(AppDestination.Dashboard)
            }

            LaunchedEffect(inventoryState.pendingDeletes) {
                if (inventoryState.pendingDeletes.isNotEmpty()) {
                    delay(4000.milliseconds)

                    if (inventoryViewModel.uiState.value.pendingDeletes.isNotEmpty()) {
                        inventoryViewModel.commitDelete()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
            ) {
                AppHeader(isConnected = isConnected)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                    onUndo = inventoryViewModel::cancelDelete
                )

                BottomAppNavigation(
                    selectedDestination = destination,
                    onDestinationSelected = { selected ->
                        destination = selected
                        if (selected == AppDestination.Dashboard) {
                            dashboardViewModel.refresh()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppHeader(
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = "Price Intelligence logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "SUPREME",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Price Intelligence",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isConnected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (isConnected) "Online" else "Offline",
                color = if (isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomAppNavigation(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    val navigationShape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(navigationShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = navigationShape
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppNavigationItem(
            destination = AppDestination.Dashboard,
            isSelected = selectedDestination == AppDestination.Dashboard,
            onClick = {
                onDestinationSelected(AppDestination.Dashboard)
            },
            modifier = Modifier.weight(1f)
        )

        AppNavigationItem(
            destination = AppDestination.Inventory,
            isSelected = selectedDestination == AppDestination.Inventory,
            onClick = {
                onDestinationSelected(AppDestination.Inventory)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AppNavigationItem(
    destination: AppDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = destination.shortLabel,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = destination.title,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
