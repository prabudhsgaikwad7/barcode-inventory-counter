package com.inventory.barcodecounter.data.repository

import com.inventory.barcodecounter.data.local.dao.InventoryCountDao
import com.inventory.barcodecounter.data.local.toDomain
import com.inventory.barcodecounter.data.local.toEntity
import com.inventory.barcodecounter.data.remote.InventoryApiService
import com.inventory.barcodecounter.data.remote.dto.SubmitCountItem
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsRequest
import com.inventory.barcodecounter.data.remote.toDomain
import com.inventory.barcodecounter.domain.InventoryMergePolicy
import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import com.inventory.barcodecounter.domain.model.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InventoryRepository(
    private val dao: InventoryCountDao,
    private val api: InventoryApiService,
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun observeCounts(status: CountStatus? = null): Flow<List<InventoryCount>> {
        val source = if (status == null) {
            dao.observeAll()
        } else {
            dao.observeByStatus(status.name)
        }
        return source.map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun lookupProduct(barcode: String): Result<ProductInfo> = runCatching {
        api.getProduct(barcode.trim()).toDomain()
    }

    sealed class SaveResult {
        data class Created(val count: InventoryCount) : SaveResult()
        data class Merged(val count: InventoryCount) : SaveResult()
        data class BlockedSubmitted(val barcode: String) : SaveResult()
    }

    suspend fun saveCount(
        barcode: String,
        productName: String,
        expectedQuantity: Int,
        actualQuantity: Int,
        expiryDate: LocalDate,
        nowMillis: Long = System.currentTimeMillis(),
    ): SaveResult {
        val normalizedBarcode = barcode.trim()
        val existingEntity = dao.findByBarcode(normalizedBarcode)

        if (existingEntity != null && existingEntity.status == CountStatus.SUBMITTED.name) {
            return SaveResult.BlockedSubmitted(normalizedBarcode)
        }

        val incoming = InventoryMergePolicy.buildNewCount(
            barcode = normalizedBarcode,
            productName = productName,
            expectedQuantity = expectedQuantity,
            actualQuantity = actualQuantity,
            expiryDate = expiryDate,
            nowMillis = nowMillis,
        )

        if (existingEntity != null && existingEntity.status == CountStatus.PENDING.name) {
            val merged = InventoryMergePolicy.merge(existingEntity.toDomain(), incoming)
            dao.update(merged.copy(id = existingEntity.id).toEntity())
            return SaveResult.Merged(merged.copy(id = existingEntity.id))
        }

        val id = dao.insert(incoming.toEntity())
        return SaveResult.Created(incoming.copy(id = id))
    }

    suspend fun submitPendingCounts(nowMillis: Long = System.currentTimeMillis()): Result<Int> = runCatching {
        val pending = dao.getAllOnce()
            .filter { it.status == CountStatus.PENDING.name }
            .map { it.toDomain() }

        if (pending.isEmpty()) return@runCatching 0

        val request = SubmitCountsRequest(
            counts = pending.map { count ->
                SubmitCountItem(
                    barcode = count.barcode,
                    productName = count.productName,
                    expectedQuantity = count.expectedQuantity,
                    actualQuantity = count.actualQuantity,
                    expiryDate = count.expiryDate.format(dateFormatter),
                )
            },
        )

        val response = api.submitCounts(request)
        check(response.success) { response.message }

        dao.markAllPendingAsSubmitted(nowMillis)
        response.submittedCount
    }

    suspend fun getAllCountsOnce(): List<InventoryCount> =
        dao.getAllOnce().map { it.toDomain() }
}
