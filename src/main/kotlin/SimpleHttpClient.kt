package org.application // Correct package declaration

import okhttp3.OkHttpClient
import okhttp3.Request


object SimpleHttpClient {
    private val client = OkHttpClient()

    fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP GET failed: ${response.code} - ${response.body?.string()}")
            }
            return response.body?.string() ?: throw Exception("No response body")
        }
    }

    // Expose the internal client for more complex requests like POST
    fun getClient(): OkHttpClient {
        return client
    }
}