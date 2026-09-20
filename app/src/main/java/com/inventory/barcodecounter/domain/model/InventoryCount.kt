package com.inventory.barcodecounter.domain.model

import java.time.LocalDate

data class InventoryCount(
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val expectedQuantity: Int,
    val actualQuantity: Int,
    val expiryDate: LocalDate,
    val status: CountStatus,
    val updatedAtMillis: Long,
) {
    val quantityDifference: Int get() = actualQuantity - expectedQuantity

    fun isExpired(today: LocalDate = LocalDate.now()): Boolean = expiryDate.isBefore(today)
}
