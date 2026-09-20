package com.inventory.barcodecounter.data.remote

import com.inventory.barcodecounter.data.remote.dto.ProductDto
import com.inventory.barcodecounter.domain.model.ProductInfo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "https://mock.inventory.local/"

    fun createApiService(): InventoryApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(MockInventoryInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InventoryApiService::class.java)
    }
}

fun ProductDto.toDomain(): ProductInfo = ProductInfo(
    barcode = barcode,
    name = name,
    expectedQuantity = expectedQuantity,
)
