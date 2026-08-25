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
import com.supreme.priceintelligence.settings.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

internal fun selectPreferredProductImageUrl(
    savedImageUrl: String?,
    amazonImageUrl: String?,
    flipkartImageUrl: String?
): String? {
    val validAmazonImage =
        amazonImageUrl?.takeIf { imageUrl -> imageUrl.isNotBlank() }
    val validSavedImage =
        savedImageUrl?.takeIf { imageUrl -> imageUrl.isNotBlank() }
    val validFlipkartImage =
        flipkartImageUrl?.takeIf { imageUrl -> imageUrl.isNotBlank() }

    return validAmazonImage ?: validSavedImage ?: validFlipkartImage
}

enum class SortOrder { MOST_VIEWED, BEST_SAVING, ALPHABETICAL, RECENT }

enum class BloomState { SUCCESS, ERROR, WARNING, NONE }

// Tapping a comparison or freshness card filters the visible product list
// down to that actionable group. Tapping the active filter clears it.
enum class PricePositionFilter {
    COMPETITIVE,
    REVIEW,
    NEEDS_CHECK
}

data class DashboardUiState(
    val searchDraft: String = "",
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.MOST_VIEWED,
    val suggestions: List<String> = emptyList(),
    val totalMatchCount: Int = 0,
    val pageItems: List<ProductCardUiState> = emptyList(),
    val manualResultLightProductIds: Set<Long> = emptySet(),
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
    val bloomState: BloomState = BloomState.NONE,
    val freshnessPromptPresented: Boolean = false
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
    private val networkMonitor: NetworkMonitor,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var connectionFeedbackJob: Job? = null
    private var automaticRefreshJob: Job? = null
    private var manualRefreshJob: Job? = null
    private var pageRefreshJob: Job? = null
    private val historyJobs = mutableMapOf<Long, Job>()
    private val manualResultLightJobs =
        mutableMapOf<Long, Job>()

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

                        if (
                            _uiState.value.bloomState ==
                            BloomState.SUCCESS
                        ) {
                            _uiState.update {
                                it.copy(
                                    bloomState = BloomState.NONE
                                )
                            }
                        }
                    }

                    startAutomaticDailyRefreshIfPossible()
                } else {
                    automaticRefreshJob?.cancel()
                    automaticRefreshJob = null
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
        _uiState.update {
            it.copy(searchDraft = query)
        }

        requestSearchSuggestions(
            query = query,
            debounceMillis =
                if (query.isBlank()) {
                    0L
                } else {
                    300L
                }
        )

        if (
            query.isBlank() &&
            _uiState.value.searchQuery.isNotBlank()
        ) {
            _uiState.update {
                it.copy(searchQuery = "")
            }

            runSearch(
                query = "",
                showLoadingIndicator = false
            )
        }
    }

    fun onSearchFocusChanged(focused: Boolean) {
        if (focused) {
            requestSearchSuggestions(
                query = _uiState.value.searchDraft,
                debounceMillis = 0L
            )
        } else {
            suggestionJob?.cancel()

            _uiState.update {
                it.copy(
                    searchDraft = it.searchQuery,
                    suggestions = emptyList()
                )
            }
        }
    }

    private fun requestSearchSuggestions(
        query: String,
        debounceMillis: Long
    ) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            if (debounceMillis > 0L) {
                delay(debounceMillis.milliseconds)
            }

            try {
                val suggestions =
                    repository.getNameSuggestions(query)

                if (query == _uiState.value.searchDraft) {
                    _uiState.update {
                        it.copy(suggestions = suggestions)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (query == _uiState.value.searchDraft) {
                    _uiState.update {
                        it.copy(suggestions = emptyList())
                    }
                }
            }
        }
    }

    fun onSearchSubmitted(query: String) {
        suggestionJob?.cancel()

        _uiState.update {
            it.copy(
                searchDraft = query,
                searchQuery = query,
                suggestions = emptyList()
            )
        }

        runSearch(query)
    }

    fun markFreshnessPromptPresented() {
        if (!_uiState.value.freshnessPromptPresented) {
            _uiState.update {
                it.copy(freshnessPromptPresented = true)
            }
        }
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
                searchDraft = "",
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

        startAutomaticDailyRefreshIfPossible()
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
        showLoadingIndicator: Boolean = true,
        debounceMillis: Long = 0L
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0L) {
                delay(debounceMillis.milliseconds)
                if (query != _uiState.value.searchQuery) {
                    return@launch
                }
            }

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
                try {
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
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A database hiccup here should show as "nothing found"
                    // rather than crash the whole app.
                    _uiState.update {
                        it.copy(totalMatchCount = 0, currentPage = 1, isLoading = false)
                    }
                    return@launch
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
        try {
            fetchPageFromDatabaseUnsafe(page)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A database hiccup while loading a page should show as an
            // empty list, not crash the whole app.
            _uiState.update {
                it.copy(pageItems = emptyList(), isLoading = false)
            }
        }
    }

    private suspend fun fetchPageFromDatabaseUnsafe(page: Int) {
        val state = _uiState.value
        val limit = state.pageSize
        val offset = (page - 1) * limit
        val sortString = state.sortOrder.name
        val priceFilter = state.priceFilter

        if (priceFilter != null) {
            // Price-position and freshness filters depend on calculated data,
            // so load the inventory once, preserve the selected sort order,
            // then filter and page the result safely in memory.
            val allMatching = repository.getPaged(
                sortOrder = sortString,
                limit = Int.MAX_VALUE,
                offset = 0
            )
            val liveById =
                state.pageItems.associateBy { card ->
                    card.item.id
                }
            val freshnessNow =
                Clock.System.now().toEpochMilliseconds()

            val filtered = allMatching.filter { item ->
                when (priceFilter) {
                    PricePositionFilter.NEEDS_CHECK ->
                        item.needsPriceCheck(
                            nowMillis = freshnessNow
                        )

                    PricePositionFilter.COMPETITIVE,
                    PricePositionFilter.REVIEW -> {
                        val liveCard = liveById[item.id]
                        val amazonPrice =
                            liveCard?.amazonResult?.price
                                ?: item.amazonLastPrice
                        val flipkartPrice =
                            liveCard?.flipkartResult?.price
                                ?: item.flipkartLastPrice

                        val position = compareWithOnlinePrices(
                            shopPrice = item.shopPrice,
                            amazonPrice = amazonPrice,
                            flipkartPrice = flipkartPrice
                        ).shopPosition

                        when (priceFilter) {
                            PricePositionFilter.COMPETITIVE ->
                                position ==
                                    ShopPricePosition.LOWER ||
                                    position ==
                                    ShopPricePosition.MATCHED

                            PricePositionFilter.REVIEW ->
                                position ==
                                    ShopPricePosition.HIGHER

                            PricePositionFilter.NEEDS_CHECK ->
                                false
                        }
                    }
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
            repository.searchPaged(
                query = state.searchQuery,
                sortOrder = sortString,
                limit = limit,
                offset = offset
            )
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
        try {
            val items = repository.getAllMatching("")
            _uiState.update { it.copy(allMatchingItems = items) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Leave allMatchingItems as it was — the Shop Overview card
            // just won't update this time, instead of crashing.
        }
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
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A failed history load should just leave the section
                // empty, not crash the whole app.
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

    fun recordProductViewed(productId: Long) {
        if (productId <= 0L) return

        viewModelScope.launch {
            try {
                repository.incrementSearchCount(productId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A popularity-count failure must never stop product details
                // from opening.
            }
        }
    }

    private fun showManualResultLight(
        productId: Long
    ) {
        manualResultLightJobs
            .remove(productId)
            ?.cancel()

        _uiState.update { state ->
            state.copy(
                manualResultLightProductIds =
                    state.manualResultLightProductIds +
                        productId
            )
        }

        val newJob = viewModelScope.launch {
            delay(5000.milliseconds)

            _uiState.update { state ->
                state.copy(
                    manualResultLightProductIds =
                        state.manualResultLightProductIds -
                            productId
                )
            }

            if (
                manualResultLightJobs[productId] ===
                coroutineContext[Job]
            ) {
                manualResultLightJobs.remove(productId)
            }
        }

        manualResultLightJobs[productId] = newJob
    }

    fun refreshProduct(productId: Long) {
        if (!_uiState.value.isConnected) {
            _uiState.update {
                it.copy(bloomState = BloomState.ERROR)
            }
            return
        }

        val pausedAutomaticJob = automaticRefreshJob
        automaticRefreshJob = null

        manualRefreshJob?.cancel()

        val newJob = viewModelScope.launch {
            pausedAutomaticJob?.cancelAndJoin()

            try {
                val hasLivePrice = scrapeOne(
                    productId = productId,
                    showFailureFeedback = true
                )

                if (hasLivePrice) {
                    showManualResultLight(productId)
                }

                markAutomaticRefreshAttempt(productId)
                refreshWholeShopSnapshot()
            } finally {
                if (
                    manualRefreshJob ===
                    coroutineContext[Job]
                ) {
                    manualRefreshJob = null
                    startAutomaticDailyRefreshIfPossible()
                }
            }
        }

        manualRefreshJob = newJob
    }

    fun refreshVisiblePrices() {
        val state = _uiState.value

        if (
            !state.isConnected ||
            state.isRefreshingPage ||
            pageRefreshJob?.isActive == true
        ) {
            return
        }

        val productIds = state.pageItems
            .filter { card ->
                !card.item.amazonUrl.isNullOrBlank() ||
                    !card.item.flipkartUrl.isNullOrBlank()
            }
            .map { card -> card.item.id }

        if (productIds.isEmpty()) return

        val pausedAutomaticJob = automaticRefreshJob
        automaticRefreshJob = null

        val newJob = viewModelScope.launch {
            pausedAutomaticJob?.cancelAndJoin()
            _uiState.update {
                it.copy(isRefreshingPage = true)
            }

            try {
                // This is an explicit user action, so a small batch is
                // acceptable. The automatic daily queue remains strictly
                // one product at a time.
                productIds.chunked(3).forEach { batch ->
                    coroutineScope {
                        batch.map { productId ->
                            async {
                                val hasLivePrice = scrapeOne(
                                    productId = productId,
                                    showFailureFeedback = true
                                )

                                if (hasLivePrice) {
                                    showManualResultLight(
                                        productId
                                    )
                                }
                            }
                        }.awaitAll()
                    }

                    batch.forEach { productId ->
                        markAutomaticRefreshAttempt(productId)
                    }
                }

                if (_uiState.value.priceFilter == null) {
                    refreshWholeShopSnapshot()
                } else {
                    fetchPageFromDatabase(1)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Individual checks already protect themselves.
            } finally {
                _uiState.update {
                    it.copy(isRefreshingPage = false)
                }

                if (
                    pageRefreshJob ===
                    coroutineContext[Job]
                ) {
                    pageRefreshJob = null
                    startAutomaticDailyRefreshIfPossible()
                }
            }
        }

        pageRefreshJob = newJob
    }

    private fun startAutomaticDailyRefreshIfPossible() {
        val preferences = appPreferences ?: return

        if (
            !_uiState.value.isConnected ||
            automaticRefreshJob?.isActive == true ||
            manualRefreshJob?.isActive == true ||
            pageRefreshJob?.isActive == true
        ) {
            return
        }

        val newJob = viewModelScope.launch {
            try {
                // Give launch, database loading, and immediate user actions
                // priority before starting background work.
                delay(4000.milliseconds)

                if (!_uiState.value.isConnected) {
                    return@launch
                }

                val now =
                    Clock.System.now().toEpochMilliseconds()

                val attemptedProductIds =
                    readAutomaticRefreshAttempts(
                        storedValue =
                            preferences
                                .automaticPriceRefreshLedger,
                        nowMillis = now
                    )

                val pendingProducts =
                    repository.getAllMatching("")
                        .filter { item ->
                            item.hasRetailerLink()
                        }
                        .filter { item ->
                            item.id !in attemptedProductIds
                        }
                        .filterNot { item ->
                            item.wasCheckedToday(now)
                        }

                pendingProducts.forEachIndexed {
                        index,
                        item ->

                    if (!_uiState.value.isConnected) {
                        return@launch
                    }

                    scrapeOne(
                        productId = item.id,
                        showFailureFeedback = false
                    )

                    markAutomaticRefreshAttempt(item.id)

                    if (index < pendingProducts.lastIndex) {
                        delay(15000.milliseconds)
                    }
                }

                refreshWholeShopSnapshot()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Automatic work must never interrupt normal app use.
            } finally {
                if (
                    automaticRefreshJob ===
                    coroutineContext[Job]
                ) {
                    automaticRefreshJob = null
                }
            }
        }

        automaticRefreshJob = newJob
    }

    private fun markAutomaticRefreshAttempt(
        productId: Long
    ) {
        val preferences = appPreferences ?: return

        if (productId <= 0L) {
            return
        }

        val now =
            Clock.System.now().toEpochMilliseconds()

        val attemptedProductIds =
            readAutomaticRefreshAttempts(
                storedValue =
                    preferences.automaticPriceRefreshLedger,
                nowMillis = now
            ) + productId

        preferences.automaticPriceRefreshLedger =
            writeAutomaticRefreshAttempts(
                productIds = attemptedProductIds,
                nowMillis = now
            )
    }

    private fun InventoryItem.hasRetailerLink(): Boolean =
        !amazonUrl.isNullOrBlank() ||
            !flipkartUrl.isNullOrBlank()

    private fun InventoryItem.wasCheckedToday(
        nowMillis: Long
    ): Boolean {
        val currentDay =
            automaticRefreshDayKey(nowMillis)

        val linkedCheckTimes = buildList {
            if (!amazonUrl.isNullOrBlank()) {
                amazonLastChecked?.let(::add)
            }

            if (!flipkartUrl.isNullOrBlank()) {
                flipkartLastChecked?.let(::add)
            }
        }

        return linkedCheckTimes.any { checkedAt ->
            checkedAt > 0L &&
                automaticRefreshDayKey(checkedAt) ==
                    currentDay
        }
    }

    private suspend fun scrapeOne(
        productId: Long,
        showFailureFeedback: Boolean = true
    ): Boolean {
        val item =
            repository.getProductById(productId)
                ?: return false

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

            if (
                !hasLivePrice &&
                hasRetailerUrl &&
                showFailureFeedback
            ) {
                _uiState.update {
                    it.copy(bloomState = BloomState.WARNING)
                }
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

            val preferredImageUrl = selectPreferredProductImageUrl(
                savedImageUrl = item.imageUrl,
                amazonImageUrl = amazonResult?.image,
                flipkartImageUrl = flipkartResult?.image
            )

            if (
                preferredImageUrl != null &&
                preferredImageUrl != item.imageUrl
            ) {
                repository.updateImageUrl(
                    itemId = productId,
                    imageUrl = preferredImageUrl
                )
            }

            // Reload the current database row after saving the price and image.
            // This preserves any edit made while the network request was running.
            // It also prevents a late result from recreating a deleted product.
            val updatedItem =
                repository.getProductById(productId)
                    ?: return false

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

            return hasLivePrice
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A single failed check — a database hiccup, unexpected data,
            // anything unforeseen — should never crash the whole app. This
            // product just keeps showing its last known price, the same as
            // a plain network failure already does.
            return false
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
