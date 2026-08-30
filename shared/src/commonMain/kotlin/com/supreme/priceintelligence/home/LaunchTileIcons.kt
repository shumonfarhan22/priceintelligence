package com.supreme.priceintelligence.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AllInbox
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StackedLineChart
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector
import com.supreme.priceintelligence.settings.LaunchTileIconStyle

internal data class LaunchTileIconSet(
    val insights: ImageVector,
    val inventory: ImageVector,
    val priceMovement: ImageVector,
    val quickCompare: ImageVector
) {
    fun asList(): List<ImageVector> = listOf(
        insights,
        inventory,
        priceMovement,
        quickCompare
    )
}

internal fun LaunchTileIconStyle.iconSet(): LaunchTileIconSet =
    when (this) {
        LaunchTileIconStyle.CLEAN ->
            LaunchTileIconSet(
                insights = Icons.Rounded.Analytics,
                inventory = Icons.Rounded.Inventory2,
                priceMovement = Icons.Rounded.QueryStats,
                quickCompare =
                    Icons.AutoMirrored.Rounded.ManageSearch
            )

        LaunchTileIconStyle.CLASSIC ->
            LaunchTileIconSet(
                insights = Icons.Rounded.Dashboard,
                inventory = Icons.Rounded.Inventory,
                priceMovement =
                    Icons.AutoMirrored.Rounded.ShowChart,
                quickCompare = Icons.Rounded.Search
            )

        LaunchTileIconStyle.BUSINESS ->
            LaunchTileIconSet(
                insights = Icons.Rounded.Assessment,
                inventory = Icons.Rounded.Warehouse,
                priceMovement = Icons.Rounded.Timeline,
                quickCompare =
                    Icons.AutoMirrored.Rounded.CompareArrows
            )

        LaunchTileIconStyle.PRODUCT ->
            LaunchTileIconSet(
                insights = Icons.Rounded.Lightbulb,
                inventory = Icons.Rounded.Category,
                priceMovement =
                    Icons.AutoMirrored.Rounded.TrendingUp,
                quickCompare = Icons.Rounded.TravelExplore
            )

        LaunchTileIconStyle.DATA ->
            LaunchTileIconSet(
                insights = Icons.Rounded.Insights,
                inventory = Icons.Rounded.AllInbox,
                priceMovement =
                    Icons.Rounded.StackedLineChart,
                quickCompare = Icons.Rounded.PriceCheck
            )
    }
