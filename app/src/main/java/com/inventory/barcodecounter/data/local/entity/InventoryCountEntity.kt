package com.inventory.barcodecounter.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_counts",
    indices = [Index(value = ["barcode"], unique = true)],
)
data class InventoryCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val productName: String,
    val expectedQuantity: Int,
    val actualQuantity: Int,
    val expiryDateEpochDay: Long,
    val status: String,
    val updatedAtMillis: Long,
)
