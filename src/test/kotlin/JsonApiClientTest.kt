package org.application // Correct package declaration

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.Gson
import org.application.data.Product
import org.application.data.ProductBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class JsonApiClientTest {

    @get:Rule
    val wireMockRule = WireMockRule(8089) // WireMock for both source and destination

    private lateinit var jsonApiClient: JsonApiClient
    private val gson = Gson()

    @Before
    fun setup() {
        jsonApiClient = JsonApiClient(HttpClient, gson)
    }

    private fun mockSourceApiProducts(products: List<Product>, status: Int = 200) {
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(gson.toJson(products))))
    }

    private fun mockDestinationApiSuccess(status: Int = 200, responseBody: String = "{\"status\": \"received\"}") {
        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)))
    }

    // --- Tests for fetchProducts ---

    @Test
    fun `test fetchProducts success`() {
        val products = listOf(
            Product("P001", "Laptop", 1200.0, "Electronics", true),
            Product("P002", "Mouse", 25.0, "Electronics", true)
        )
        mockSourceApiProducts(products)

        val fetchedProducts = jsonApiClient.fetchProducts("http://localhost:8089/api/products")

        assertEquals(2, fetchedProducts.size)
        assertEquals("Laptop", fetchedProducts[0].name)
    }

    @Test(expected = Exception::class)
    fun `test fetchProducts when source API returns 404`() {
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(404)
                .withBody("Not Found")))

        jsonApiClient.fetchProducts("http://localhost:8089/api/products")
    }

    @Test(expected = Exception::class)
    fun `test fetchProducts when source API returns empty body`() {
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(""))) // Empty body

        jsonApiClient.fetchProducts("http://localhost:8089/api/products")
    }

    // --- Tests for sendProductBatch ---

    @Test
    fun `test sendProductBatch success`() {
        val products = listOf(
            Product("P003", "Keyboard", 75.0, "Electronics", true)
        )
        val batch = ProductBatch("B001", products, 1678886400000L) // Example timestamp

        mockDestinationApiSuccess()

        val response = jsonApiClient.sendProductBatch("http://localhost:8089/api/product-batches", batch)

        assertEquals("{\"status\": \"received\"}", response)

        // Verify that the POST request was made with the correct JSON body
        verify(postRequestedFor(urlEqualTo("/api/product-batches"))
            .withHeader("Content-Type", equalTo("application/json; charset=utf-8"))
            .withRequestBody(equalToJson(gson.toJson(batch), true, true)) // true, true for ignoreArrayOrder, ignoreExtraElements
        )
    }

    @Test
    fun `test sendProductBatch with incorrect JSON structure (WireMock's perspective)`() {
        // We'll mock a scenario where the destination API only accepts a specific structure.
        // If our client sends something different, WireMock won't match, and our client will get a 404.
        stubFor(post(urlEqualTo("/api/product-batches"))
            .withRequestBody(equalToJson("""
                {
                    "batchId": "B001",
                    "products": [
                        { "id": "P003", "name": "Keyboard", "price": 75.0, "category": "Electronics", "inStock": true }
                    ],
                    "timestamp": 1678886400000
                }
            """, true, true)) // Exact JSON match expected by the mock
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("Correctly received batch!")))

        val products = listOf(
            Product("P003", "Keyboard", 75.0, "Electronics", true)
        )
        val batch = ProductBatch("B001", products, 1678886400000L) // Example timestamp

        // Now, let's send a batch that *slightly* differs to show a mismatch
        val incorrectBatch = ProductBatch("B001", products, 1234567890000L) // Different timestamp

        try {
            jsonApiClient.sendProductBatch("http://localhost:8089/api/product-batches", incorrectBatch)
            // If we reach here, it means the API accepted the incorrect JSON, which is not what we want for this test.
            // Fail the test if no exception is thrown.
            org.junit.Assert.fail("Expected an exception for incorrect JSON structure, but none was thrown.")
        } catch (e: Exception) {
            assertTrue("Expected a failure due to API mismatch (WireMock's 404 behavior for unmatched request)",
                e.message!!.contains("Failed to send product batch: 404 - "))
        }

        // Verify that no request matching the *expected* exact JSON was received
        verify(0, postRequestedFor(urlEqualTo("/api/product-batches"))
            .withRequestBody(equalToJson(gson.toJson(batch), true, true)))
    }

    @Test(expected = Exception::class)
    fun `test sendProductBatch when destination API returns 500`() {
        val products = listOf(
            Product("P004", "Monitor", 300.0, "Electronics", false)
        )
        val batch = ProductBatch("B002", products, System.currentTimeMillis())

        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")))

        jsonApiClient.sendProductBatch("http://localhost:8089/api/product-batches", batch)
    }

    // --- Tests for processProductsAndSendBatch ---

    @Test
    fun `test processProductsAndSendBatch success`() {
        val sourceProducts = listOf(
            Product("P005", "Webcam", 50.0, "Peripherals", true),
            Product("P006", "Microphone", 70.0, "Peripherals", false)
        )
        mockSourceApiProducts(sourceProducts) // Mock source API response

        mockDestinationApiSuccess() // Mock destination API success

        val batchId = "BATCH_XYZ"
        val response = jsonApiClient.processProductsAndSendBatch(
            "http://localhost:8089/api/products",
            "http://localhost:8089/api/product-batches",
            batchId
        )

        assertEquals("{\"status\": \"received\"}", response)

        // Verify the request made to the destination API
        verify(postRequestedFor(urlEqualTo("/api/product-batches"))
            .withHeader("Content-Type", equalTo("application/json; charset=utf-8"))
            .withRequestBody(matchingJsonPath("$.batchId", equalTo(batchId))) // Check specific fields
            .withRequestBody(matchingJsonPath("$.products[0].name", equalTo("Webcam"))) // Access array elements directly
            .withRequestBody(matchingJsonPath("$.products[1].id", equalTo("P006")))
            .withRequestBody(matchingJsonPath("$.timestamp", matching("^[0-9]+$"))) // Ensure timestamp is a number
        )
    }

    /*@Test(expected = Exception::class)
    fun `test processProductsAndSendBatch when source API fails`() {
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(HttpURLConnection.FORBIDDEN)
                .withBody("Access Denied")))

        // Destination API mock (won't be hit if source fails)
        mockDestinationApiSuccess()

        jsonApiClient.processProductsAndSendBatch(
            "http://localhost:8089/api/products",
            "http://localhost:8089/api/product-batches",
            "BATCH_FAIL_SOURCE"
        )
    }

    @Test(expected = Exception::class)
    fun `test processProductsAndSendBatch when destination API fails`() {
        val sourceProducts = listOf(
            Product("P007", "Speaker", 150.0, "Audio", true)
        )
        mockSourceApiProducts(sourceProducts) // Source API succeeds

        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                .withStatus(HttpURLConnection.BAD_GATEWAY)
                .withBody("Service Unavailable")))

        jsonApiClient.processProductsAndSendBatch(
            "http://localhost:8089/api/products",
            "http://localhost:8089/api/product-batches",
            "BATCH_FAIL_DEST"
        )
    }*/
}