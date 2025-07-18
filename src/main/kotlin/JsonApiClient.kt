package org.application

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.application.data.Product
import org.application.data.ProductBatch
import okhttp3.Request
import java.util.UUID

/**
 * A client for interacting with JSON-based APIs to fetch product data
 * and send product batches. It leverages OkHttp for HTTP requests and Gson
 * for JSON serialization/deserialization.
 *
 * @param httpClient An instance of [HttpClient] used to execute HTTP requests.
 * @param gson An optional [Gson] instance for JSON processing. Defaults to a new Gson instance.
 */
class JsonApiClient(
    private val httpClient: HttpClient,
    private val gson: Gson = Gson()
) {

    /**
     * Fetches a list of products from a specified source API URL.
     * It performs an HTTP GET request and parses the JSON response into a list of [Product] objects.
     *
     * @param url The URL of the source API endpoint to fetch products from.
     * @return A [List] of [Product] objects parsed from the JSON response.
     * @throws Exception if the HTTP GET request fails or if JSON parsing encounters an error.
     */
    fun fetchProducts(url: String): List<Product> {
        // Execute an HTTP GET request to the provided URL and get the JSON response string.
        val jsonResponse = httpClient.get(url)
        // Define the type for Gson to correctly deserialize a List of Product objects.
        val type = object : TypeToken<List<Product>>() {}.type
        // Use Gson to convert the JSON string into a List<Product>.
        return gson.fromJson(jsonResponse, type)
    }

    /**
     * Sends a [ProductBatch] to a specified destination API URL using an HTTP POST request.
     * The batch object is serialized into a JSON body.
     *
     * @param url The URL of the destination API endpoint to send the product batch to.
     * @param batch The [ProductBatch] object to be sent.
     * @return The response body from the destination API as a [String] if the request is successful.
     * @throws Exception if the HTTP POST request is unsuccessful (e.g., non-2xx status code)
     * or if the response body is unexpectedly null.
     */
    fun sendProductBatch(url: String, batch: ProductBatch): String {
        // Convert the ProductBatch object to a JSON string.
        val jsonBody = gson.toJson(batch)
        // Define the media type for the request body as JSON with UTF-8 charset.
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        // Create an OkHttp RequestBody from the JSON string and media type.
        val requestBody = jsonBody.toRequestBody(mediaType)

        // Build the OkHttp POST request.
        val request = Request.Builder() // Explicitly use okhttp3.Request.Builder
            .url(url) // Set the target URL.
            .post(requestBody) // Set the request method to POST with the JSON body.
            .build() // Build the final request object.

        // Execute the request and handle the response. The 'use' block ensures the response body is closed.
        httpClient.getClient().newCall(request).execute().use { response ->
            // Check if the HTTP response was successful (2xx status code).
            if (!response.isSuccessful) {
                // If not successful, throw an exception with details about the failure.
                throw Exception("Failed to send product batch: ${response.code} - ${response.body?.string()}")
            }
            // If successful, return the response body as a string.
            // Use Elvis operator (?:) to throw an exception if the response body is null.
            return response.body?.string() ?: throw Exception("No response body for sent batch")
        }
    }

    /**
     * A composite function that first fetches products from a source API,
     * then constructs a [ProductBatch] from the fetched products, and finally
     * sends this batch to a destination API.
     * This method encapsulates a common workflow of data retrieval and submission.
     *
     * @param sourceUrl The URL of the API to fetch products from.
     * @param destinationUrl The URL of the API to send the product batch to.
     * @param batchId A unique identifier to assign to the newly created [ProductBatch].
     * @return The response body from the destination API after successfully sending the batch.
     * @throws Exception if fetching products fails, or if sending the product batch fails.
     */
    fun processProductsAndSendBatch(sourceUrl: String, destinationUrl: String, batchId: UUID): String {
        // Step 1: Fetch products from the source URL.
        val products = fetchProducts(sourceUrl)
        // Step 2: Create a ProductBatch using the fetched products and the provided batchId and current timestamp.
        val productBatch = ProductBatch(
            batchId = batchId,
            products = products,
            timestamp = System.currentTimeMillis() // Use current time for the batch timestamp.
        )
        // Step 3: Send the created ProductBatch to the destination URL.
        return sendProductBatch(destinationUrl, productBatch)
    }
}