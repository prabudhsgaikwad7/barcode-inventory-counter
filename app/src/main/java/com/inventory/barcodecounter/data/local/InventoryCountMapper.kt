package com.inventory.barcodecounter.data.local

import com.inventory.barcodecounter.data.local.entity.InventoryCountEntity
import com.inventory.barcodecounter.domain.model.CountStatus
import com.inventory.barcodecounter.domain.model.InventoryCount
import java.time.LocalDate

fun InventoryCountEntity.toDomain(): InventoryCount = InventoryCount(
    id = id,
    barcode = barcode,
    productName = productName,
    expectedQuantity = expectedQuantity,
    actualQuantity = actualQuantity,
    expiryDate = LocalDate.ofEpochDay(expiryDateEpochDay),
    status = CountStatus.valueOf(status),
    updatedAtMillis = updatedAtMillis,
)

fun InventoryCount.toEntity(): InventoryCountEntity = InventoryCountEntity(
    id = id,
    barcode = barcode,
    productName = productName,
    expectedQuantity = expectedQuantity,
    actualQuantity = actualQuantity,
    expiryDateEpochDay = expiryDate.toEpochDay(),
    status = status.name,
    updatedAtMillis = updatedAtMillis,
)
