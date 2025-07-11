package org.application

import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * `SimpleHttpClient` is an object (singleton) that provides basic HTTP GET
 * functionality using the OkHttp library.
 *
 * It's designed to perform synchronous HTTP GET requests and handle responses,
 * throwing exceptions for non-successful status codes or for successful
 * responses that contain no body content.
 */
object HttpClient {
    /**
     * The underlying OkHttpClient instance used for making HTTP requests.
     * This client is initialized once when the `SimpleHttpClient` object is first accessed.
     */
    private val client = OkHttpClient()

    /**
     * Executes a synchronous HTTP GET request to the specified URL.
     *
     * This function expects a successful HTTP response (2xx status code)
     * with a non-empty body.
     *
     * @param url The URL to which the GET request will be sent.
     * @return The response body as a [String] if the request is successful and contains content.
     * @throws Exception If the HTTP request fails (non-2xx status code) or
     * if a successful response (2xx) contains no content (null or empty body).
     * The exception message will include the HTTP status code and, if available,
     * the error body for failed requests, or an indication of missing content.
     */
    fun get(url: String): String {
        // Build the HTTP GET request
        val request = Request.Builder()
            .url(url) // Set the URL for the request
            .build() // Finalize the request object

        // Execute the request and handle the response using a 'use' block for automatic resource closing
        client.newCall(request).execute().use { response ->
            // Check if the HTTP response indicates success (2xx status code)
            if (!response.isSuccessful) {
                // If not successful, throw an exception with the status code and error body
                throw Exception("HTTP GET failed: ${response.code} - ${response.body?.string()}")
            }
            // If successful, attempt to read the response body
            val responseBody = response.body?.string()
            // If the response body is null or empty after a successful request, throw an exception
            if (responseBody.isNullOrEmpty()) {
                throw Exception("No content in response body for successful request (Status: ${response.code})")
            }
            // Return the non-empty response body
            return responseBody
        }
    }

    /**
     * Provides direct access to the underlying [OkHttpClient] instance.
     *
     * This method can be used by other components that need more advanced
     * HTTP capabilities not directly exposed by `SimpleHttpClient`'s `get` method,
     * such as making POST requests or configuring interceptors.
     *
     * @return The [OkHttpClient] instance used by `SimpleHttpClient`.
     */
    fun getClient(): OkHttpClient {
        return client
    }
}