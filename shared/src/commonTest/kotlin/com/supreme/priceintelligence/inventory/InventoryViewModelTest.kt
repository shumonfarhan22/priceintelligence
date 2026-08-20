package com.supreme.priceintelligence.inventory

import androidx.lifecycle.viewModelScope
import com.supreme.priceintelligence.data.FakeInventoryDao
import com.supreme.priceintelligence.data.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
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
    fun selectedProductsMoveIntoTheExistingUndoDeleteFlow() = runTest(dispatcher) {
        val repository = populatedRepository()
        val viewModel = InventoryViewModel(repository)
        advanceUntilIdle()

        val firstTwoIds = viewModel.uiState.value.products.take(2).map { item -> item.id }
        firstTwoIds.forEach(viewModel::toggleSelection)
        assertEquals(firstTwoIds.toSet(), viewModel.uiState.value.selectedItemIds)

        viewModel.queueSelectedForDelete()
        assertTrue(viewModel.uiState.value.selectedItemIds.isEmpty())
        assertEquals(firstTwoIds.toSet(), viewModel.uiState.value.pendingDeletes.map { it.id }.toSet())

        viewModel.cancelDelete()
        assertTrue(viewModel.uiState.value.pendingDeletes.isEmpty())
        assertEquals(3, repository.getAllRecent().size)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun changingSearchKeepsSelectionLimitedToShownProducts() = runTest(dispatcher) {
        val repository = populatedRepository()
        val viewModel = InventoryViewModel(repository)
        advanceUntilIdle()

        viewModel.selectAllVisible()
        assertEquals(3, viewModel.uiState.value.selectedItemIds.size)

        viewModel.onDirectoryQueryChanged("Galaxy")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.products.size)
        assertEquals(
            viewModel.uiState.value.products.map { item -> item.id }.toSet(),
            viewModel.uiState.value.selectedItemIds
        )

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun anotherQuickDeleteJoinsTheSameUndoWindow() = runTest(dispatcher) {
        val repository = populatedRepository()
        val viewModel = InventoryViewModel(repository)
        advanceUntilIdle()
        val products = viewModel.uiState.value.products

        viewModel.queueDelete(setOf(products[0]))
        viewModel.queueDelete(setOf(products[1]))

        assertEquals(
            setOf(products[0].id, products[1].id),
            viewModel.uiState.value.pendingDeletes.map { item -> item.id }.toSet()
        )

        viewModel.viewModelScope.cancel()
    }

    private suspend fun populatedRepository(): InventoryRepository {
        val repository = InventoryRepository(FakeInventoryDao())
        repository.addProduct(name = "Samsung Galaxy", shopPrice = 50_000.0)
        repository.addProduct(name = "Apple Charger", shopPrice = 2_000.0)
        repository.addProduct(name = "Sony Headphones", shopPrice = 8_000.0)
        return repository
    }
}
