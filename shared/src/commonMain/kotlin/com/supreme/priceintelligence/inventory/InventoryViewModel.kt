@file:OptIn(ExperimentalTime::class)

package com.supreme.priceintelligence.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.InventoryItem
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
    val expandedGroups: Set<String> = emptySet()
)

// Was AndroidViewModel(application) — now takes the repository through the
// constructor, same pattern as DashboardViewModel. Backup export/import is
// deliberately left out for now — see the note near the bottom.
class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

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
            _uiState.update { it.copy(products = repository.getAllRecent()) }
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

            _uiState.update { it.copy(products = results, isRefreshing = false) }
        }
    }

    fun onDirectoryQueryChanged(query: String) {
        _uiState.update { it.copy(directoryQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250.milliseconds)
            val results = if (query.isBlank()) repository.getAllRecent() else repository.search(query)
            _uiState.update { it.copy(products = results) }
        }
    }

    fun onFormFieldChanged(
        productName: String? = null,
        shopPrice: String? = null,
        barcode: String? = null,
        amazonUrl: String? = null,
        flipkartUrl: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    productName = productName ?: state.form.productName,
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
                form = InventoryFormState(
                    editingItem = item,
                    productName = item.productName,
                    shopPrice = item.shopPrice.toString(),
                    barcode = item.barcode.orEmpty(),
                    amazonUrl = item.amazonUrl.orEmpty(),
                    flipkartUrl = item.flipkartUrl.orEmpty()
                )
            )
        }
    }

    fun clearForm() {
        _uiState.update { it.copy(form = InventoryFormState()) }
    }

    fun saveProduct(onSuccess: () -> Unit = {}) {
        val form = _uiState.value.form
        val price = form.shopPrice.toDoubleOrNull()

        if (form.productName.isBlank()) {
            showStatus("Product name is required", isError = true)
            return
        }
        if (price == null) {
            showStatus("Enter a valid shop price", isError = true)
            return
        }

        viewModelScope.launch {
            val cleanAmazon = form.amazonUrl.trim().ifBlank { null }
            val cleanFlipkart = form.flipkartUrl.trim().ifBlank { null }
            val cleanBarcode = form.barcode.trim().ifBlank { null }
            val currentId = form.editingItem?.id ?: 0L

            if (cleanAmazon != null && repository.isAmazonUrlDuplicate(cleanAmazon, currentId)) {
                showStatus("This Amazon link is already used by another product", isError = true)
                return@launch
            }

            if (cleanFlipkart != null && repository.isFlipkartUrlDuplicate(cleanFlipkart, currentId)) {
                showStatus("This Flipkart link is already used by another product", isError = true)
                return@launch
            }

            try {
                val savedId: Long
                if (form.isEditing) {
                    savedId = form.editingItem!!.id
                    repository.updateProduct(
                        form.editingItem.copy(
                            productName = form.productName.trim(),
                            shopPrice = price,
                            barcode = cleanBarcode,
                            amazonUrl = cleanAmazon,
                            flipkartUrl = cleanFlipkart
                        )
                    )
                    showStatus("Product updated")
                } else {
                    savedId = repository.addProduct(
                        name = form.productName.trim(),
                        shopPrice = price,
                        barcode = cleanBarcode,
                        amazonUrl = cleanAmazon,
                        flipkartUrl = cleanFlipkart
                    )
                    showStatus("Product added")
                }
                clearForm()

                val brandGroup = form.productName.trim().substringBefore(" ").uppercase()
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
        _uiState.update { it.copy(pendingDeletes = items) }
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

    // --- BACKUP EXPORT / IMPORT: intentionally not ported yet ---
    // The old versions took an android.net.Uri from Android's Storage Access
    // Framework, which has no iOS equivalent. This gets its own dedicated
    // expect/actual step later, same treatment as the barcode scanner.

    private fun showStatus(message: String, isError: Boolean = false, isInfo: Boolean = false) {
        statusClearJob?.cancel()
        _uiState.update { it.copy(statusMessage = message, statusIsError = isError, statusIsInfo = isInfo) }
        statusClearJob = viewModelScope.launch {
            delay(5000.milliseconds)
            _uiState.update { it.copy(statusMessage = null, statusIsError = false, statusIsInfo = false) }
        }
    }
}