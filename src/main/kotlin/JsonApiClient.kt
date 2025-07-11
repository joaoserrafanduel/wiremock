package org.application // Correct package declaration

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.application.data.Product
import org.application.data.ProductBatch

class JsonApiClient(
    private val httpClient: HttpClient,
    private val gson: Gson = Gson()
) {

    // Function to fetch a list of products from a source API
    fun fetchProducts(url: String): List<Product> {
        val jsonResponse = httpClient.get(url)
        val type = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(jsonResponse, type)
    }

    // Function to send a ProductBatch to a destination API
    fun sendProductBatch(url: String, batch: ProductBatch): String {
        val jsonBody = gson.toJson(batch)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.getClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to send product batch: ${response.code} - ${response.body?.string()}")
            }
            return response.body?.string() ?: throw Exception("No response body for sent batch")
        }
    }

    // New function combining fetch and send
    fun processProductsAndSendBatch(sourceUrl: String, destinationUrl: String, batchId: String): String {
        val products = fetchProducts(sourceUrl)
        val productBatch = ProductBatch(
            batchId = batchId,
            products = products,
            timestamp = System.currentTimeMillis()
        )
        return sendProductBatch(destinationUrl, productBatch)
    }
}