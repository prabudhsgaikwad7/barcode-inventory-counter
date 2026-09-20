package com.inventory.barcodecounter.domain

import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import java.time.LocalDate

object InventoryMergePolicy {
    /**
     * Merges a new count into an existing pending record for the same barcode.
     * Actual quantities are summed; expiry uses the earlier date (stricter for inventory).
     */
    fun merge(existing: InventoryCount, incoming: InventoryCount): InventoryCount {
        require(existing.barcode == incoming.barcode) { "Barcodes must match to merge" }
        require(existing.status == CountStatus.PENDING) { "Only pending records can be merged" }

        return existing.copy(
            productName = incoming.productName.ifBlank { existing.productName },
            expectedQuantity = incoming.expectedQuantity,
            actualQuantity = existing.actualQuantity + incoming.actualQuantity,
            expiryDate = minOf(existing.expiryDate, incoming.expiryDate),
            updatedAtMillis = incoming.updatedAtMillis,
        )
    }

    fun buildNewCount(
        barcode: String,
        productName: String,
        expectedQuantity: Int,
        actualQuantity: Int,
        expiryDate: LocalDate,
        nowMillis: Long,
    ): InventoryCount = InventoryCount(
        barcode = barcode.trim(),
        productName = productName,
        expectedQuantity = expectedQuantity,
        actualQuantity = actualQuantity,
        expiryDate = expiryDate,
        status = CountStatus.PENDING,
        updatedAtMillis = nowMillis,
    )
}
