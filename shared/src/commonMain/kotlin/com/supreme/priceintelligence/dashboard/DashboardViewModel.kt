@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.InventoryRepository
import com.supreme.priceintelligence.data.PriceHistoryEntry
import com.supreme.priceintelligence.data.PriceRetailer
import com.supreme.priceintelligence.network.NetworkMonitor
import com.supreme.priceintelligence.network.PriceFetcher
import com.supreme.priceintelligence.network.ScrapeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

enum class SortOrder { MOST_VIEWED, BEST_SAVING, ALPHABETICAL, RECENT }

enum class BloomState { SUCCESS, ERROR, WARNING, NONE }

// Tapping the Competitive or Review KPI card on the decision summary sets
// this, filtering the visible product list down to just that bucket.
enum class PricePositionFilter { COMPETITIVE, REVIEW }

data class DashboardUiState(
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.MOST_VIEWED,
    val suggestions: List<String> = emptyList(),
    val totalMatchCount: Int = 0,
    val pageItems: List<ProductCardUiState> = emptyList(),
    val allMatchingItems: List<InventoryItem> = emptyList(),
    val priceFilter: PricePositionFilter? = null,
    val refreshCollapseTick: Int = 0,
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val priceHistoryByProduct: Map<Long, List<PriceHistoryEntry>> = emptyMap(),
    val historyLoadingProductIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshingPage: Boolean = false,
    val isConnected: Boolean = false,
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
    private val scraper: PriceFetcher,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var connectionFeedbackJob: Job? = null
    private val historyJobs = mutableMapOf<Long, Job>()

    init {
        runSearch("")
        monitorNetwork()
    }

    private fun monitorNetwork() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    _uiState.update {
                        it.copy(
                            isConnected = true,
                            bloomState = BloomState.SUCCESS
                        )
                    }
                    connectionFeedbackJob?.cancel()
                    connectionFeedbackJob = viewModelScope.launch {
                        delay(5000.milliseconds)
                        if (_uiState.value.bloomState == BloomState.SUCCESS) {
                            _uiState.update { it.copy(bloomState = BloomState.NONE) }
                        }
                    }
                } else {
                    connectionFeedbackJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            bloomState = BloomState.ERROR
                        )
                    }
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

        // Actually re-run the product search as the user types, the same
        // way Inventory's search already does. Before this, typing only
        // refreshed the suggestions dropdown above — the visible product
        // list never updated until the user explicitly submitted, which is
        // why a partial name looked like it matched nothing.
        runSearch(query, showLoadingIndicator = false)
    }

    fun onSearchSubmitted(query: String) {
        _uiState.update { it.copy(searchQuery = query, suggestions = emptyList()) }
        runSearch(query)
    }

    fun refresh() {
        // Pull-to-refresh is a "start over" gesture — a filter left active
        // from before would be surprising to still see after asking for a
        // fresh look at everything. Same for an expanded card: refreshing
        // should hand back the compact view. Same for a typed search: it
        // should clear too, back to the plain, unfiltered product list.
        suggestionJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                suggestions = emptyList(),
                priceFilter = null,
                refreshCollapseTick = it.refreshCollapseTick + 1
            )
        }
        runSearch(
            query = "",
            showLoadingIndicator = true
        )
    }

    fun refreshSilently() {
        runSearch(
            query = _uiState.value.searchQuery,
            showLoadingIndicator = false
        )
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        runSearch(_uiState.value.searchQuery)
    }

    // Tapping an active filter again clears it; tapping the other one
    // switches straight to it.
    fun setPriceFilter(filter: PricePositionFilter) {
        val current = _uiState.value.priceFilter
        _uiState.update { it.copy(priceFilter = if (current == filter) null else filter) }
        runSearch(_uiState.value.searchQuery)
    }

    private fun runSearch(
        query: String,
        showLoadingIndicator: Boolean = true
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (showLoadingIndicator) {
                _uiState.update {
                    it.copy(isLoading = true)
                }
            }

            if (query.isNotBlank() && _uiState.value.priceFilter != null) {
                // The Shop Overview card (and its Competitive/Review filter)
                // only exists on the unsearched, "main" view — clear it the
                // moment a real search starts, so a hidden filter can't
                // silently keep narrowing the search results.
                _uiState.update { it.copy(priceFilter = null) }
            }

            if (_uiState.value.priceFilter == null) {
                val startTime =
                    Clock.System.now().toEpochMilliseconds()
                val count = repository.getSearchCount(query)
                _uiState.update {
                    it.copy(
                        totalMatchCount = count,
                        currentPage = 1,
                        searchDurationMs = Clock.System.now().toEpochMilliseconds() - startTime
                    )
                }
            } else {
                // A KPI filter is active: fetchPageFromDatabase computes the
                // filtered count itself below, since it depends on each
                // product's live/last-known price position, not just text.
                _uiState.update { it.copy(currentPage = 1) }
            }
            fetchPageFromDatabase(1)
        }
    }

    fun goToPage(page: Int) {
        val safePage = page.coerceIn(1, _uiState.value.totalPages)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(currentPage = safePage, isLoading = true) }
            fetchPageFromDatabase(safePage)
        }
    }

    private suspend fun fetchPageFromDatabase(page: Int) {
        val state = _uiState.value
        val limit = state.pageSize
        val offset = (page - 1) * limit
        val sortString = state.sortOrder.name
        val priceFilter = state.priceFilter

        if (priceFilter != null) {
            // The database can sort and page by text/sort order, but not by
            // a computed price position — that depends on live results too.
            // So pull every matching product once and filter/page it here.
            val allMatching = repository.getAllMatching("")
            val liveById = state.pageItems.associateBy { card -> card.item.id }

            val filtered = allMatching.filter { item ->
                val liveCard = liveById[item.id]
                val amazonPrice = liveCard?.amazonResult?.price ?: item.amazonLastPrice
                val flipkartPrice = liveCard?.flipkartResult?.price ?: item.flipkartLastPrice
                val position = compareWithOnlinePrices(
                    shopPrice = item.shopPrice,
                    amazonPrice = amazonPrice,
                    flipkartPrice = flipkartPrice
                ).shopPosition

                when (priceFilter) {
                    PricePositionFilter.COMPETITIVE ->
                        position == ShopPricePosition.LOWER || position == ShopPricePosition.MATCHED

                    PricePositionFilter.REVIEW ->
                        position == ShopPricePosition.HIGHER
                }
            }

            val pageSlice = filtered.drop(offset).take(limit)

            _uiState.update {
                val pageIds = pageSlice.map { item -> item.id }.toSet()
                it.copy(
                    totalMatchCount = filtered.size,
                    pageItems = pageSlice.map { item -> ProductCardUiState(item = item) },
                    allMatchingItems = allMatching,
                    priceHistoryByProduct = it.priceHistoryByProduct.filterKeys { id -> id in pageIds },
                    historyLoadingProductIds = it.historyLoadingProductIds.filter { id -> id in pageIds }.toSet(),
                    isLoading = false
                )
            }
            return
        }

        val pageProducts = if (state.searchQuery.isBlank()) {
            repository.getPaged(sortString, limit, offset)
        } else {
            val searchResults = repository.searchPaged(state.searchQuery, sortString, limit, offset)
            repository.incrementSearchCountBulk(searchResults.map { it.id })
            searchResults
        }

        _uiState.update {
            val pageIds = pageProducts.map { item -> item.id }.toSet()
            it.copy(
                pageItems = pageProducts.map { item -> ProductCardUiState(item = item) },
                priceHistoryByProduct = it.priceHistoryByProduct.filterKeys { id -> id in pageIds },
                historyLoadingProductIds = it.historyLoadingProductIds.filter { id -> id in pageIds }.toSet(),
                isLoading = false
            )
        }

        refreshWholeShopSnapshot()
    }

    // Powers the whole-shop decision summary. Kept separate from the paged
    // list above so that card, which promises a full-shop view, is never
    // quietly limited to whatever page the user happens to be looking at.
    private suspend fun refreshWholeShopSnapshot() {
        // Always the whole shop, regardless of any active text search — the
        // Shop Overview card is hidden while searching (see the screen), so
        // this must never narrow down to just the search results.
        val items = repository.getAllMatching("")
        _uiState.update { it.copy(allMatchingItems = items) }
    }

    fun loadPriceHistory(productId: Long, force: Boolean = false) {
        if (productId <= 0L) return
        if (!force && _uiState.value.priceHistoryByProduct.containsKey(productId)) return

        historyJobs.remove(productId)?.cancel()
        historyJobs[productId] = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    historyLoadingProductIds = state.historyLoadingProductIds + productId
                )
            }
            try {
                val history = repository.getPriceHistory(productId)
                _uiState.update { state ->
                    state.copy(
                        priceHistoryByProduct = state.priceHistoryByProduct + (productId to history)
                    )
                }
            } finally {
                _uiState.update { state ->
                    state.copy(
                        historyLoadingProductIds = state.historyLoadingProductIds - productId
                    )
                }
                historyJobs.remove(productId)
            }
        }
    }

    fun refreshProduct(productId: Long) {
        if (!_uiState.value.isConnected) {
            _uiState.update { it.copy(bloomState = BloomState.ERROR) }
            return
        }

        viewModelScope.launch {
            repository.incrementSearchCount(productId)
            scrapeOne(productId)
            refreshWholeShopSnapshot()
        }
    }

    fun refreshVisiblePrices() {
        val state = _uiState.value
        if (!state.isConnected || state.isRefreshingPage) return

        val productIds = state.pageItems
            .filter { card ->
                !card.item.amazonUrl.isNullOrBlank() ||
                    !card.item.flipkartUrl.isNullOrBlank()
            }
            .map { card -> card.item.id }

        if (productIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPage = true) }
            try {
                // Three products at a time keeps the stores and slower phones from
                // being flooded with up to twenty simultaneous web requests.
                productIds.chunked(3).forEach { batch ->
                    coroutineScope {
                        batch.map { productId ->
                            async { scrapeOne(productId) }
                        }.awaitAll()
                    }
                }
                refreshWholeShopSnapshot()
            } finally {
                _uiState.update { it.copy(isRefreshingPage = false) }
            }
        }
    }

    private suspend fun scrapeOne(productId: Long) {
        val item = _uiState.value.pageItems.find { it.item.id == productId }?.item ?: return
        setCardRefreshing(productId, true)

        try {
            val (amazonResult, flipkartResult) = coroutineScope {
                val amazonDeferred = async {
                    item.amazonUrl
                        ?.takeIf { url -> url.isNotBlank() }
                        ?.let { url -> scraper.fetchPrice(url) }
                }
                val flipkartDeferred = async {
                    item.flipkartUrl
                        ?.takeIf { url -> url.isNotBlank() }
                        ?.let { url -> scraper.fetchPrice(url) }
                }
                amazonDeferred.await() to flipkartDeferred.await()
            }

            val hasRetailerUrl =
                !item.amazonUrl.isNullOrBlank() || !item.flipkartUrl.isNullOrBlank()
            val hasLivePrice =
                amazonResult?.price != null || flipkartResult?.price != null

            if (!hasLivePrice && hasRetailerUrl) {
                _uiState.update { it.copy(bloomState = BloomState.WARNING) }
                viewModelScope.launch {
                    delay(4000.milliseconds)
                    if (_uiState.value.bloomState == BloomState.WARNING) {
                        _uiState.update { it.copy(bloomState = BloomState.NONE) }
                    }
                }
            }

            val now = Clock.System.now().toEpochMilliseconds()
            amazonResult?.price?.let { price ->
                repository.recordPriceCheck(
                    itemId = productId,
                    retailer = PriceRetailer.AMAZON,
                    price = price,
                    checkedAt = now
                )
            }
            flipkartResult?.price?.let { price ->
                repository.recordPriceCheck(
                    itemId = productId,
                    retailer = PriceRetailer.FLIPKART,
                    price = price,
                    checkedAt = now
                )
            }

            val refreshedHistory = if (hasLivePrice) {
                repository.getPriceHistory(productId)
            } else {
                null
            }

            val newImage = amazonResult?.image ?: flipkartResult?.image
            var updatedItem = item.copy(
                amazonLastPrice = amazonResult?.price ?: item.amazonLastPrice,
                amazonLastChecked = if (amazonResult?.price != null) now else item.amazonLastChecked,
                flipkartLastPrice = flipkartResult?.price ?: item.flipkartLastPrice,
                flipkartLastChecked = if (flipkartResult?.price != null) now else item.flipkartLastChecked
            )
            if (newImage != null && newImage != item.imageUrl) {
                updatedItem = updatedItem.copy(imageUrl = newImage)
                repository.updateProduct(updatedItem)
            }

            _uiState.update { state ->
                state.copy(
                    priceHistoryByProduct = if (refreshedHistory == null) {
                        state.priceHistoryByProduct
                    } else {
                        state.priceHistoryByProduct + (productId to refreshedHistory)
                    },
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
        } finally {
            setCardRefreshing(productId, false)
        }
    }

    private fun setCardRefreshing(productId: Long, isRefreshing: Boolean) {
        _uiState.update { state ->
            state.copy(
                pageItems = state.pageItems.map { card ->
                    if (card.item.id == productId) card.copy(isRefreshing = isRefreshing) else card
                }
            )
        }
    }
}
