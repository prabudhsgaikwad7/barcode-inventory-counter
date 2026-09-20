package com.inventory.barcodecounter.ui

import com.inventory.barcodecounter.data.local.dao.InventoryCountDao
import com.inventory.barcodecounter.data.local.entity.InventoryCountEntity
import com.inventory.barcodecounter.data.remote.InventoryApiService
import com.inventory.barcodecounter.data.remote.dto.ProductDto
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsRequest
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsResponse
import com.inventory.barcodecounter.data.repository.InventoryRepository
import com.inventory.barcodecounter.domain.model.CountStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeViewModelDao
    private lateinit var fakeApi: FakeViewModelApi
    private lateinit var repository: InventoryRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeViewModelDao()
        fakeApi = FakeViewModelApi()
        repository = InventoryRepository(fakeDao, fakeApi)
        viewModel = InventoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onBarcodeChange_updatesFormState() {
        viewModel.onBarcodeChange("123456")
        assertEquals("123456", viewModel.uiState.value.form.barcode)
    }

    @Test
    fun lookupProduct_success_fillsProductDetails() = runTest {
        viewModel.setBarcodeFromScan("1234567890123")
        testDispatcher.scheduler.advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertTrue(form.productLoaded)
        assertEquals("Organic Milk 1L", form.productName)
        assertEquals("48", form.expectedQuantity)
    }

    @Test
    fun lookupProduct_failure_allowsManualEntry() = runTest {
        viewModel.lookupProduct("999999")
        testDispatcher.scheduler.advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertTrue(form.productLoaded)
        assertEquals("Unlisted Item", form.productName)
    }

    @Test
    fun saveCount_validatesExpiredProduct() = runTest {
        viewModel.setBarcodeFromScan("1234567890123")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onActualQuantityChange("40")
        viewModel.onExpiryDateChange(LocalDate.now().minusDays(1).toString())

        viewModel.saveCount()
        testDispatcher.scheduler.advanceUntilIdle()

        val message = viewModel.uiState.value.message
        assertNotNull(message)
        assertTrue(message!!.contains("expired"))
    }

    @Test
    fun selectTab_updatesSelectedTab() {
        viewModel.selectTab(RecordsTab.SUBMITTED)
        assertEquals(RecordsTab.SUBMITTED, viewModel.uiState.value.selectedTab)
    }
}

private class FakeViewModelDao : InventoryCountDao {
    private val entities = mutableListOf<InventoryCountEntity>()

    override fun observeAll(): Flow<List<InventoryCountEntity>> = flowOf(entities)

    override fun observeByStatus(status: String): Flow<List<InventoryCountEntity>> =
        flowOf(entities.filter { it.status == status })

    override suspend fun findPendingByBarcode(barcode: String): InventoryCountEntity? =
        entities.find { it.barcode == barcode && it.status == CountStatus.PENDING.name }

    override suspend fun findByBarcode(barcode: String): InventoryCountEntity? =
        entities.find { it.barcode == barcode }

    override suspend fun insert(entity: InventoryCountEntity): Long {
        val id = (entities.size + 1).toLong()
        entities.add(entity.copy(id = id))
        return id
    }

    override suspend fun update(entity: InventoryCountEntity) {
        val idx = entities.indexOfFirst { it.id == entity.id }
        if (idx != -1) entities[idx] = entity
    }

    override suspend fun markAllPendingAsSubmitted(updatedAt: Long) {
        for (i in entities.indices) {
            if (entities[i].status == CountStatus.PENDING.name) {
                entities[i] = entities[i].copy(status = CountStatus.SUBMITTED.name, updatedAtMillis = updatedAt)
            }
        }
    }

    override suspend fun getAllOnce(): List<InventoryCountEntity> = entities.toList()
}

private class FakeViewModelApi : InventoryApiService {
    override suspend fun getProduct(barcode: String): ProductDto {
        if (barcode == "1234567890123") {
            return ProductDto("1234567890123", "Organic Milk 1L", 48)
        }
        throw Exception("Not found")
    }

    override suspend fun submitCounts(request: SubmitCountsRequest): SubmitCountsResponse {
        return SubmitCountsResponse(success = true, submittedCount = request.counts.size, message = "OK")
    }
}
