package com.inventory.barcodecounter.data.remote

import com.inventory.barcodecounter.data.remote.dto.ProductDto
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsRequest
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InventoryApiService {
    @GET("products/{barcode}")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductDto

    @POST("counts/submit")
    suspend fun submitCounts(@Body request: SubmitCountsRequest): SubmitCountsResponse
}
