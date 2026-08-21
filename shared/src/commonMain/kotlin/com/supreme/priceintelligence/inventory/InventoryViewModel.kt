@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.InventoryItem
import com.supreme.priceintelligence.data.InventoryBackupManager
import com.supreme.priceintelligence.data.BackupImportResult
import com.supreme.priceintelligence.data.InventoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.milliseconds

data class InventoryFormState(
    val editingItem: InventoryItem? = null,
    val productName: String = "",
    val purchaseCost: String = "",
    val shopPrice: String = "",
    val barcode: String = "",
    val amazonUrl: String = "",
    val flipkartUrl: String = ""
) {
    val isEditing: Boolean get() = editingItem != null
}

data class InventoryUiState(
    val form: InventoryFormState = InventoryFormState(),
    val directoryQuery: String = "",
    val products: List<InventoryItem> = emptyList(),
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    val statusIsInfo: Boolean = false,
    val highlightedItemId: Long? = null,
    val isRefreshing: Boolean = false,
    val pendingDeletes: Set<InventoryItem> = emptySet(),
    val expandedGroups: Set<String> = emptySet(),
    val selectedItemIds: Set<Long> = emptySet()
) {
    val isSelectionMode: Boolean get() = selectedItemIds.isNotEmpty()
}

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val backupManager = InventoryBackupManager(repository)

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var statusClearJob: Job? = null

    init {
        loadAll()
    }

    fun toggleGroup(groupName: String) {
        _uiState.update { state ->
            val newExpanded = if (state.expandedGroups.contains(groupName)) {
                state.expandedGroups - groupName
            } else {
                state.expandedGroups + groupName
            }
            state.copy(expandedGroups = newExpanded)
        }
    }

    private fun loadAll() {
        viewModelScope.launch {
            updateVisibleProducts(repository.getAllRecent())
        }
    }

    fun refreshInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(400.milliseconds)

            val query = _uiState.value.directoryQuery
            val results = if (query.isBlank()) {
                repository.getAllRecent()
            } else {
                repository.search(query)
            }

            updateVisibleProducts(results, isRefreshing = false)
        }
    }

    fun onDirectoryQueryChanged(query: String) {
        _uiState.update { it.copy(directoryQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250.milliseconds)
            val results = if (query.isBlank()) repository.getAllRecent() else repository.search(query)
            updateVisibleProducts(results)
        }
    }

    fun toggleSelection(itemId: Long) {
        _uiState.update { state ->
            if (state.products.none { item -> item.id == itemId }) return@update state
            state.copy(
                selectedItemIds = if (itemId in state.selectedItemIds) {
                    state.selectedItemIds - itemId
                } else {
                    state.selectedItemIds + itemId
                }
            )
        }
    }

    fun selectAllVisible() {
        _uiState.update { state ->
            val pendingIds = state.pendingDeletes.map { item -> item.id }.toSet()
            state.copy(
                selectedItemIds = state.products
                    .map { item -> item.id }
                    .filterNot { id -> id in pendingIds }
                    .toSet()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { state -> state.copy(selectedItemIds = emptySet()) }
    }

    fun queueSelectedForDelete() {
        val state = _uiState.value
        val selectedItems = state.products
            .filter { item -> item.id in state.selectedItemIds }
            .toSet()
        if (selectedItems.isNotEmpty()) queueDelete(selectedItems)
    }

    fun onFormFieldChanged(
        productName: String? = null,
        purchaseCost: String? = null,
        shopPrice: String? = null,
        barcode: String? = null,
        amazonUrl: String? = null,
        flipkartUrl: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    productName = productName ?: state.form.productName,
                    purchaseCost =
                        purchaseCost ?: state.form.purchaseCost,
                    shopPrice = shopPrice ?: state.form.shopPrice,
                    barcode = barcode ?: state.form.barcode,
                    amazonUrl = amazonUrl ?: state.form.amazonUrl,
                    flipkartUrl = flipkartUrl ?: state.form.flipkartUrl
                )
            )
        }
    }

    fun startEditing(item: InventoryItem) {
        _uiState.update {
            it.copy(
                selectedItemIds = emptySet(),
                form = InventoryFormState(
                    editingItem = item,
                    productName = item.productName,
                    purchaseCost =
                        item.purchaseCost?.toString().orEmpty(),
                    shopPrice = item.shopPrice.toString(),
                    barcode = item.barcode.orEmpty(),
                    amazonUrl = item.amazonUrl.orEmpty(),
                    flipkartUrl = item.flipkartUrl.orEmpty()
                )
            )
        }
    }

    fun clearFormFields() {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    productName = "",
                    purchaseCost = "",
                    shopPrice = "",
                    barcode = "",
                    amazonUrl = "",
                    flipkartUrl = ""
                )
            )
        }
    }

    fun clearForm() {
        _uiState.update { it.copy(form = InventoryFormState()) }
    }

    fun saveProduct(onSuccess: () -> Unit = {}) {
        val form = _uiState.value.form
        val validation = validateInventoryInput(
            productName = form.productName,
            shopPrice = form.shopPrice,
            barcode = form.barcode,
            amazonUrl = form.amazonUrl,
            flipkartUrl = form.flipkartUrl,
            purchaseCost = form.purchaseCost
        )
        val input = validation.input
        if (input == null) {
            showStatus(validation.errorMessage ?: "Check the product details", isError = true)
            return
        }

        viewModelScope.launch {
            val currentId = form.editingItem?.id ?: 0L

            if (input.amazonUrl != null && repository.isAmazonUrlDuplicate(input.amazonUrl, currentId)) {
                showStatus("This Amazon link is already used by another product", isError = true)
                return@launch
            }

            if (input.flipkartUrl != null && repository.isFlipkartUrlDuplicate(input.flipkartUrl, currentId)) {
                showStatus("This Flipkart link is already used by another product", isError = true)
                return@launch
            }

            try {
                val savedId: Long
                if (form.isEditing) {
                    savedId = form.editingItem!!.id
                    repository.updateProduct(
                        form.editingItem.copy(
                            productName = input.productName,
                            shopPrice = input.shopPrice,
                            purchaseCost = input.purchaseCost,
                            barcode = input.barcode,
                            amazonUrl = input.amazonUrl,
                            flipkartUrl = input.flipkartUrl
                        )
                    )
                    showStatus("Product updated")
                } else {
                    savedId = repository.addProduct(
                        name = input.productName,
                        shopPrice = input.shopPrice,
                        purchaseCost = input.purchaseCost,
                        barcode = input.barcode,
                        amazonUrl = input.amazonUrl,
                        flipkartUrl = input.flipkartUrl
                    )
                    showStatus("Product added")
                }
                clearForm()

                val brandGroup = input.productName.substringBefore(" ").uppercase()
                _uiState.update { it.copy(
                    highlightedItemId = savedId,
                    expandedGroups = it.expandedGroups + brandGroup
                ) }
                loadAll()

                onSuccess()

                delay(5000.milliseconds)
                _uiState.update { it.copy(highlightedItemId = null) }
            } catch (_: androidx.sqlite.SQLiteException) {
                // Most likely cause: this barcode already belongs to another product.
                showStatus("That barcode is already used by another product", isError = true)
            }
        }
    }

    fun queueDelete(items: Set<InventoryItem>) {
        val itemIds = items.map { item -> item.id }.toSet()
        _uiState.update {
            it.copy(
                pendingDeletes = it.pendingDeletes + items,
                selectedItemIds = it.selectedItemIds - itemIds
            )
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeletes = emptySet()) }
    }

    fun commitDelete() {
        val ids = _uiState.value.pendingDeletes.map { it.id }.toSet()
        if (ids.isEmpty()) return

        _uiState.update { state ->
            state.copy(
                pendingDeletes = emptySet(),
                products = state.products.filter { it.id !in ids }
            )
        }

        viewModelScope.launch {
            ids.forEach { repository.deleteProduct(it) }
            if (_uiState.value.form.editingItem?.id in ids) clearForm()
            loadAll()
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, statusIsError = false, statusIsInfo = false) }
    }

    suspend fun createBackupJson(): String = backupManager.createBackupJson()

    suspend fun restoreBackupJson(contents: String): BackupImportResult {
        val result = backupManager.importBackupJson(contents)
        loadAll()

        val details = buildList {
            add("${result.addedCount} product(s) added")
            if (result.duplicateCount > 0) add("${result.duplicateCount} duplicate(s) skipped")
            if (result.invalidCount > 0) add("${result.invalidCount} invalid row(s) skipped")
        }.joinToString(" • ")
        showStatus(
            message = "Restore complete: $details",
            isInfo = result.addedCount == 0
        )
        return result
    }

    fun reportBackupSaved() {
        showStatus("Backup saved successfully")
    }

    fun reportBackupError(message: String) {
        showStatus(message, isError = true)
    }

    fun reportError(message: String) {
        showStatus(message, isError = true)
    }

    private fun showStatus(message: String, isError: Boolean = false, isInfo: Boolean = false) {
        statusClearJob?.cancel()
        _uiState.update { it.copy(statusMessage = message, statusIsError = isError, statusIsInfo = isInfo) }
        statusClearJob = viewModelScope.launch {
            delay(5000.milliseconds)
            _uiState.update { it.copy(statusMessage = null, statusIsError = false, statusIsInfo = false) }
        }
    }

    private fun updateVisibleProducts(
        products: List<InventoryItem>,
        isRefreshing: Boolean = _uiState.value.isRefreshing
    ) {
        val visibleIds = products.map { item -> item.id }.toSet()
        _uiState.update { state ->
            state.copy(
                products = products,
                isRefreshing = isRefreshing,
                selectedItemIds = state.selectedItemIds.intersect(visibleIds)
            )
        }
    }
}
