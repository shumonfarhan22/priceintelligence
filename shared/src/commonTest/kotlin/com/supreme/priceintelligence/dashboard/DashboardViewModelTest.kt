package com.supreme.priceintelligence.dashboard

import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.FakeInventoryDao
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceFetcher
import com.supreme.priceintelligence.network.ScrapeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun liveRefreshUpdatesTheVisibleCardAndDatabaseCache() = runTest(dispatcher) {
        val dao = FakeInventoryDao()
        val repository = InventoryRepository(dao)
        repository.addProduct(
            name = "Test phone",
            shopPrice = 50_000.0,
            amazonUrl = "https://amazon.in/test",
            flipkartUrl = "https://flipkart.com/test"
        )
        val fetcher = FakePriceFetcher(
            prices = mapOf(
                "https://amazon.in/test" to 48_000.0,
                "https://flipkart.com/test" to 49_000.0
            )
        )
        val viewModel = DashboardViewModel(
            repository = repository,
            scraper = fetcher,
            networkMonitor = FakeNetworkMonitor(isConnected = true)
        )

        advanceUntilIdle()
        val productId = viewModel.uiState.value.pageItems.single().item.id
        viewModel.refreshProduct(productId)
        advanceUntilIdle()

        val card = viewModel.uiState.value.pageItems.single()
        assertEquals(48_000.0, card.amazonResult?.price)
        assertEquals(49_000.0, card.flipkartResult?.price)
        assertFalse(card.isRefreshing)
        assertEquals(48_000.0, dao.getAllRanked().single().amazonLastPrice)
        assertEquals(49_000.0, dao.getAllRanked().single().flipkartLastPrice)
        assertEquals(2, fetcher.requestedUrls.size)
        val history = repository.getPriceHistory(productId)
        assertEquals(2, history.size)
        assertEquals(
            setOf(48_000.0, 49_000.0),
            history.map { entry -> entry.price }.toSet()
        )

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun offlineRefreshKeepsSavedDataAndMakesNoRequest() = runTest(dispatcher) {
        val dao = FakeInventoryDao()
        val repository = InventoryRepository(dao)
        repository.addProduct(
            name = "Offline phone",
            shopPrice = 10_000.0,
            amazonUrl = "https://amazon.in/offline"
        )
        val fetcher = FakePriceFetcher(emptyMap())
        val viewModel = DashboardViewModel(
            repository = repository,
            scraper = fetcher,
            networkMonitor = FakeNetworkMonitor(isConnected = false)
        )

        advanceUntilIdle()
        val productId = viewModel.uiState.value.pageItems.single().item.id
        viewModel.refreshProduct(productId)
        advanceUntilIdle()

        assertTrue(fetcher.requestedUrls.isEmpty())
        assertEquals(BloomState.ERROR, viewModel.uiState.value.bloomState)
        assertFalse(viewModel.uiState.value.pageItems.single().isRefreshing)

        viewModel.viewModelScope.cancel()
    }
}

private class FakePriceFetcher(
    private val prices: Map<String, Double>
) : PriceFetcher {
    val requestedUrls = mutableListOf<String>()

    override suspend fun fetchPrice(url: String): ScrapeResult {
        requestedUrls += url
        return ScrapeResult(price = prices[url])
    }
}

private class FakeNetworkMonitor(
    isConnected: Boolean
) : NetworkMonitor {
    private val connection = MutableStateFlow(isConnected)
    override val isConnected: StateFlow<Boolean> = connection

    override fun stop() = Unit
}
