package com.inventory.barcodecounter.data.remote.dto

data class ProductDto(
    val barcode: String,
    val name: String,
    val expectedQuantity: Int,
)

data class SubmitCountsRequest(
    val counts: List<SubmitCountItem>,
)

data class SubmitCountItem(
    val barcode: String,
    val productName: String,
    val expectedQuantity: Int,
    val actualQuantity: Int,
    val expiryDate: String,
)

data class SubmitCountsResponse(
    val success: Boolean,
    val submittedCount: Int,
    val message: String,
)
