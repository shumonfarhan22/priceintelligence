@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceScraper
import com.supreme.priceintelligence.network.ScrapeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.milliseconds

data class ProductCardUiState(
    val item: InventoryItem,
    val isRefreshing: Boolean = false,
    val amazonResult: ScrapeResult? = null,
    val flipkartResult: ScrapeResult? = null
)

enum class SortOrder { MOST_VIEWED, ALPHABETICAL, RECENT }

enum class BloomState { SUCCESS, ERROR, WARNING, NONE }

data class DashboardUiState(
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.MOST_VIEWED,
    val suggestions: List<String> = emptyList(),
    val totalMatchCount: Int = 0,
    val pageItems: List<ProductCardUiState> = emptyList(),
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val isLoading: Boolean = false,
    val searchDurationMs: Long = 0,
    val bloomState: BloomState = BloomState.NONE
) {
    val totalPages: Int
        get() = if (totalMatchCount == 0) 1 else ((totalMatchCount - 1) / pageSize) + 1
}

// Was AndroidViewModel(application) — now everything it used to reach for via
// Application (the repository, the scraper, the network monitor) gets handed
// in through the constructor instead, since shared code has no Application/Context.
class DashboardViewModel(
    private val repository: InventoryRepository,
    private val scraper: PriceScraper,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null

    init {
        runSearch("")
        monitorNetwork()
    }

    private fun monitorNetwork() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    _uiState.update { it.copy(bloomState = BloomState.SUCCESS) }
                    delay(5000.milliseconds)
                    if (_uiState.value.bloomState == BloomState.SUCCESS) {
                        _uiState.update { it.copy(bloomState = BloomState.NONE) }
                    }
                } else {
                    _uiState.update { it.copy(bloomState = BloomState.ERROR) }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(250.milliseconds)
            _uiState.update { it.copy(suggestions = repository.getNameSuggestions(query)) }
        }
    }

    fun onSearchSubmitted(query: String) {
        _uiState.update { it.copy(searchQuery = query, suggestions = emptyList()) }
        runSearch(query)
    }

    fun refresh() {
        runSearch(_uiState.value.searchQuery)
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        runSearch(_uiState.value.searchQuery)
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val startTime = Clock.System.now().toEpochMilliseconds()
            val count = repository.getSearchCount(query)
            _uiState.update {
                it.copy(
                    totalMatchCount = count,
                    currentPage = 1,
                    searchDurationMs = Clock.System.now().toEpochMilliseconds() - startTime
                )
            }
            fetchPageFromDatabase(1)
        }
    }

    fun goToPage(page: Int) {
        _uiState.update { it.copy(currentPage = page, isLoading = true) }
        viewModelScope.launch {
            fetchPageFromDatabase(page)
        }
    }

    private suspend fun fetchPageFromDatabase(page: Int) {
        val state = _uiState.value
        val limit = state.pageSize
        val offset = (page - 1) * limit
        val sortString = state.sortOrder.name

        val pageProducts = if (state.searchQuery.isBlank()) {
            repository.getPaged(sortString, limit, offset)
        } else {
            val searchResults = repository.searchPaged(state.searchQuery, sortString, limit, offset)
            repository.incrementSearchCountBulk(searchResults.map { it.id })
            searchResults
        }

        _uiState.update {
            it.copy(
                pageItems = pageProducts.map { item -> ProductCardUiState(item = item) },
                isLoading = false
            )
        }
    }

    fun refreshProduct(productId: Long) {
        viewModelScope.launch {
            repository.incrementSearchCount(productId)
            scrapeOne(productId)
        }
    }

    private suspend fun scrapeOne(productId: Long) {
        setCardRefreshing(productId)

        val item = _uiState.value.pageItems.find { it.item.id == productId }?.item ?: return

        val amazonDeferred = viewModelScope.async { scraper.fetchPrice(item.amazonUrl.orEmpty()) }
        val flipkartDeferred = viewModelScope.async { scraper.fetchPrice(item.flipkartUrl.orEmpty()) }
        val (amazonResult, flipkartResult) = listOf(amazonDeferred, flipkartDeferred).awaitAll()

        val hasAmazonUrl = item.amazonUrl?.isNotEmpty() == true
        val hasFlipkartUrl = item.flipkartUrl?.isNotEmpty() == true
        if (amazonResult.price == null && flipkartResult.price == null && (hasAmazonUrl || hasFlipkartUrl)) {
            _uiState.update { it.copy(bloomState = BloomState.WARNING) }
            viewModelScope.launch {
                delay(4000.milliseconds)
                if (_uiState.value.bloomState == BloomState.WARNING) {
                    _uiState.update { it.copy(bloomState = BloomState.NONE) }
                }
            }
        }

        // --- PRICE MEMORY BANK ---
        val now = Clock.System.now().toEpochMilliseconds()
        if (amazonResult.price != null) {
            repository.updateAmazonCache(productId, amazonResult.price, now)
        }
        if (flipkartResult.price != null) {
            repository.updateFlipkartCache(productId, flipkartResult.price, now)
        }

        val newImage = amazonResult.image ?: flipkartResult.image
        var updatedItem = item
        if (newImage != null && newImage != item.imageUrl) {
            updatedItem = item.copy(imageUrl = newImage)
            repository.updateProduct(updatedItem)
        }

        _uiState.update { state ->
            state.copy(
                pageItems = state.pageItems.map { card ->
                    if (card.item.id == productId) {
                        card.copy(
                            item = updatedItem,
                            isRefreshing = false,
                            amazonResult = amazonResult,
                            flipkartResult = flipkartResult
                        )
                    } else card
                }
            )
        }
    }

    private fun setCardRefreshing(productId: Long) {
        _uiState.update { state ->
            state.copy(
                pageItems = state.pageItems.map { card ->
                    if (card.item.id == productId) card.copy(isRefreshing = true) else card
                }
            )
        }
    }
}
