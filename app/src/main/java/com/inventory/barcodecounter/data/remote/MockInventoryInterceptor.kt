package com.inventory.barcodecounter.data.remote

import com.google.gson.Gson
import com.inventory.barcodecounter.data.remote.dto.ProductDto
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsRequest
import com.inventory.barcodecounter.data.remote.dto.SubmitCountsResponse
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Mock backend for product lookup and count submission (no real server required).
 */
class MockInventoryInterceptor : Interceptor {
    private val gson = Gson()

    private val catalog = mapOf(
        "1234567890123" to ProductDto("1234567890123", "Organic Milk 1L", 48),
        "9876543210987" to ProductDto("9876543210987", "Whole Wheat Bread", 24),
        "5556667778889" to ProductDto("5556667778889", "Canned Tomatoes", 120),
        "1112223334445" to ProductDto("1112223334445", "Hand Sanitizer 500ml", 60),
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.trimEnd('/')

        val (code, body) = when {
            path.startsWith("/products/") -> {
                val barcode = path.removePrefix("/products/")
                val product = catalog[barcode]
                if (product == null) {
                    404 to """{"error":"Product not found"}"""
                } else {
                    200 to gson.toJson(product)
                }
            }

            path == "/counts/submit" && request.method == "POST" -> {
                val raw = request.body?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    buffer.readUtf8()
                }.orEmpty()
                val submitRequest = gson.fromJson(raw, SubmitCountsRequest::class.java)
                val response = SubmitCountsResponse(
                    success = true,
                    submittedCount = submitRequest.counts.size,
                    message = "Counts accepted",
                )
                200 to gson.toJson(response)
            }

            else -> 404 to """{"error":"Unknown endpoint"}"""
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody(JSON))
            .build()
    }

    companion object {
        private val JSON = "application/json".toMediaType()
    }
}
