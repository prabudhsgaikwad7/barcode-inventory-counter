package com.inventory.barcodecounter.data.repository

import com.inventory.barcodecounter.data.local.dao.InventoryCountDao
import com.inventory.barcodecounter.data.local.entity.InventoryCountEntity
import com.inventory.barcodecounter.data.remote.InventoryApiService
import com.inventory.barcodecounter.data.remote.dto.ProductDto
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsRequest
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsResponse
import com.inventory.barcodecounter.domain.model.CountStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class InventoryRepositoryTest {

    private lateinit var fakeDao: FakeInventoryCountDao
    private lateinit var fakeApi: FakeInventoryApiService
    private lateinit var repository: InventoryRepository

    @Before
    fun setup() {
        fakeDao = FakeInventoryCountDao()
        fakeApi = FakeInventoryApiService()
        repository = InventoryRepository(fakeDao, fakeApi)
    }

    @Test
    fun saveCount_createsNewPendingCount() = runTest {
        val result = repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 8,
            expiryDate = LocalDate.of(2026, 12, 31),
        )

        assertTrue(result is InventoryRepository.SaveResult.Created)
        val created = (result as InventoryRepository.SaveResult.Created).count
        assertEquals("1001", created.barcode)
        assertEquals(8, created.actualQuantity)
        assertEquals(CountStatus.PENDING, created.status)
    }

    @Test
    fun saveCount_mergesExistingPendingCount() = runTest {
        repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 5,
            expiryDate = LocalDate.of(2026, 12, 31),
        )

        val result = repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 3,
            expiryDate = LocalDate.of(2026, 11, 15),
        )

        assertTrue(result is InventoryRepository.SaveResult.Merged)
        val merged = (result as InventoryRepository.SaveResult.Merged).count
        assertEquals(8, merged.actualQuantity)
        assertEquals(LocalDate.of(2026, 11, 15), merged.expiryDate)
    }

    @Test
    fun saveCount_blocksIfBarcodeAlreadySubmitted() = runTest {
        repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 10,
            expiryDate = LocalDate.of(2026, 12, 31),
        )
        repository.submitPendingCounts()

        val result = repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 2,
            expiryDate = LocalDate.of(2026, 12, 31),
        )

        assertTrue(result is InventoryRepository.SaveResult.BlockedSubmitted)
        assertEquals("1001", (result as InventoryRepository.SaveResult.BlockedSubmitted).barcode)
    }

    @Test
    fun submitPendingCounts_marksPendingAsSubmitted() = runTest {
        repository.saveCount(
            barcode = "1001",
            productName = "Apple Juice",
            expectedQuantity = 10,
            actualQuantity = 8,
            expiryDate = LocalDate.of(2026, 12, 31),
        )

        val submittedCountResult = repository.submitPendingCounts()
        assertTrue(submittedCountResult.isSuccess)
        assertEquals(1, submittedCountResult.getOrNull())

        val all = repository.getAllCountsOnce()
        assertEquals(1, all.size)
        assertEquals(CountStatus.SUBMITTED, all.first().status)
    }
}

private class FakeInventoryCountDao : InventoryCountDao {
    private val entities = mutableListOf<InventoryCountEntity>()
    private var idCounter = 1L

    override fun observeAll(): Flow<List<InventoryCountEntity>> = flowOf(entities)

    override fun observeByStatus(status: String): Flow<List<InventoryCountEntity>> =
        flowOf(entities.filter { it.status == status })

    override suspend fun findPendingByBarcode(barcode: String): InventoryCountEntity? =
        entities.find { it.barcode == barcode && it.status == CountStatus.PENDING.name }

    override suspend fun findByBarcode(barcode: String): InventoryCountEntity? =
        entities.find { it.barcode == barcode }

    override suspend fun insert(entity: InventoryCountEntity): Long {
        val newEntity = entity.copy(id = idCounter++)
        entities.add(newEntity)
        return newEntity.id
    }

    override suspend fun update(entity: InventoryCountEntity) {
        val index = entities.indexOfFirst { it.id == entity.id }
        if (index != -1) {
            entities[index] = entity
        }
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

private class FakeInventoryApiService : InventoryApiService {
    override suspend fun getProduct(barcode: String): ProductDto {
        return ProductDto(barcode, "Mock Product", 10)
    }

    override suspend fun submitCounts(request: SubmitCountsRequest): SubmitCountsResponse {
        return SubmitCountsResponse(success = true, submittedCount = request.counts.size, message = "Submitted")
    }
}
