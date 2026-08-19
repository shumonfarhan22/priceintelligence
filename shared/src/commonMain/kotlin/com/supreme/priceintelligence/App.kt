package com.supreme.priceintelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
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

@Composable
fun App(databaseBuilder: RoomDatabase.Builder<AppDatabase>) {
    MaterialTheme {
        // remember{} means this only runs once per app session, not on every recomposition
        val repository = remember { InventoryRepository(getRoomDatabase(databaseBuilder).inventoryDao()) }
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