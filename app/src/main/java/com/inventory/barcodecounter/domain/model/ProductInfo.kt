package com.inventory.barcodecounter.domain.model

data class ProductInfo(
    val barcode: String,
    val name: String,
    val expectedQuantity: Int,
)
