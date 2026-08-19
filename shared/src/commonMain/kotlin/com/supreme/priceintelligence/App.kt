package com.supreme.priceintelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.room3.RoomDatabase
import com.supreme.priceintelligence.data.AppDatabase
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.getRoomDatabase
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceScraper

// networkMonitor is built by each platform's own entry point (MainActivity /
// MainViewController) and handed in here, same idea as databaseBuilder — since
// AndroidNetworkMonitor needs a Context and IosNetworkMonitor doesn't, there's
// no single shared way to construct one from inside commonMain.
@Composable
fun App(
    databaseBuilder: RoomDatabase.Builder<AppDatabase>,
    networkMonitor: NetworkMonitor
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val repository = remember { InventoryRepository(getRoomDatabase(databaseBuilder).inventoryDao()) }
            val scraper = remember { PriceScraper() }
            var productCount by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(Unit) {
                productCount = repository.getTotalCount()
            }

            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (val count = productCount) {
                        null -> "Loading your inventory..."
                        else -> "Room is alive — $count product(s) in the database."
                    }
                )
            }
        }
    }
}