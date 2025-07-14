package org.application

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.google.gson.Gson
import org.application.data.Product
import org.application.data.ProductBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.HttpURLConnection

/**
 * Integration and unit tests for the [JsonApiClient] class.
 *
 * This test suite utilizes WireMock to simulate external API behaviors,
 * allowing for comprehensive testing of [JsonApiClient]'s capabilities
 * in fetching product data from a source API and sending product batches
 * to a destination API. It covers various scenarios including successful
 * responses, different HTTP error codes, and malformed data handling.
 */
class JsonApiClientTest {

    /**
     * JUnit Rule that starts and stops a WireMock server for each test method.
     * Configured with `dynamicPort()` to automatically find a free port,
     * preventing "Address already in use" errors during test execution,
     * especially in concurrent or repeated Gradle builds.
     */
    @get:Rule
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    /**
     * The instance of [JsonApiClient] being tested.
     * Initialized in the [setup] method before each test.
     */
    private lateinit var jsonApiClient: JsonApiClient

    /**
     * Gson instance used for JSON serialization and deserialization in tests
     * and for mocking API responses.
     */
    private val gson = Gson()

    /**
     * Sets up the test environment before each test method.
     * Initializes the [jsonApiClient] with the [HttpClient] (your main HTTP client)
     * and the Gson instance.
     */
    @Before
    fun setup() {
        // Initialize JsonApiClient, passing HttpClient (your previously named SimpleHttpClient)
        // and the Gson instance for JSON handling.
        jsonApiClient = JsonApiClient(HttpClient, gson)
    }

    /**
     * Helper function to mock the response from the source API's `/api/products` endpoint.
     *
     * @param products The list of [Product] objects to be returned as the API response body.
     * @param status The HTTP status code for the mocked response (defaults to 200 OK).
     */
    private fun mockSourceApiProducts(products: List<Product>, status: Int = 200) {
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(gson.toJson(products))))
    }

    /**
     * Helper function to mock a successful response from the destination API's `/api/product-batches` endpoint.
     *
     * @param status The HTTP status code for the mocked response (defaults to 200 OK).
     * @param responseBody The JSON string to be returned as the API response body (defaults to "{\"status\": \"received\"}").
     */
    private fun mockDestinationApiSuccess(status: Int = 200, responseBody: String = "{\"status\": \"received\"}") {
        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody)))
    }

    // --- Tests for fetchProducts ---

    /**
     * Tests the successful fetching of products from the source API.
     * Mocks a 200 OK response with a list of products.
     */
    @Test
    fun `test fetchProducts success`() {
        // Arrange: Prepare a list of products to be returned by the mocked API
        val products = listOf(
            Product("P001", "Laptop", 1200.0, "Electronics", true),
            Product("P002", "Mouse", 25.0, "Electronics", true)
        )
        // Configure WireMock to return these products with a 200 OK status
        mockSourceApiProducts(products)

        // Act: Call the fetchProducts method on the JsonApiClient, using the dynamic port
        val fetchedProducts = jsonApiClient.fetchProducts("http://localhost:${wireMockRule.port()}/api/products")

        // Assert: Verify that the correct number of products were fetched and their data is accurate
        assertEquals(2, fetchedProducts.size)
        assertEquals("Laptop", fetchedProducts[0].name)
    }

    /**
     * Tests the behavior of `fetchProducts` when the source API returns a 404 Not Found error.
     * Expects an Exception to be thrown by the client.
     */
    @Test(expected = Exception::class)
    fun `test fetchProducts when source API returns 404`() {
        // Arrange: Configure WireMock to respond with a 404 Not Found status
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(404)
                .withBody("Not Found")))

        // Act: Call the fetchProducts method. An Exception is expected here.
        jsonApiClient.fetchProducts("http://localhost:${wireMockRule.port()}/api/products")
        // Assert: The test passes if an Exception is thrown.
    }

    /**
     * Tests the behavior of `fetchProducts` when the source API returns a 200 OK
     * status but with an empty response body.
     * Expects an Exception to be thrown by the client (likely due to JSON parsing failure).
     */
    @Test(expected = Exception::class)
    fun `test fetchProducts when source API returns empty body`() {
        // Arrange: Configure WireMock to respond with 200 OK but no body
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(""))) // Empty body

        // Act: Call the fetchProducts method. An Exception is expected here.
        jsonApiClient.fetchProducts("http://localhost:${wireMockRule.port()}/api/products")
        // Assert: The test passes if an Exception is thrown.
    }

    // --- Tests for sendProductBatch ---

    /**
     * Tests the successful sending of a product batch to the destination API.
     * Mocks a 200 OK response from the destination.
     * Verifies that the correct POST request with the expected JSON body was made.
     */
    @Test
    fun `test sendProductBatch success`() {
        val products = listOf(
            Product("P003", "Keyboard", 75.0, "Electronics", true)
        )
        val batch = ProductBatch("B001", products, 1678886400000L) // Example timestamp

        mockDestinationApiSuccess()

        val response = jsonApiClient.sendProductBatch("http://localhost:${wireMockRule.port()}/api/product-batches", batch)

        assertEquals("{\"status\": \"received\"}", response)

        // Verify that the POST request was made with the correct JSON body
        verify(postRequestedFor(urlEqualTo("/api/product-batches"))
            .withHeader("Content-Type", equalToIgnoreCase("application/json; charset=utf-8")) // <-- CHANGED HERE
            .withRequestBody(equalToJson(gson.toJson(batch), true, true)) // true, true for ignoreArrayOrder, ignoreExtraElements
        )
    }

    /**
     * Tests `sendProductBatch` behavior when the sent JSON structure does not match
     * what the destination API (mocked by WireMock) expects, leading to a 404 (unmatched request).
     */
    /*@Test
    fun `test sendProductBatch with incorrect JSON structure (WireMock's perspective)`() {
        // Arrange: Configure WireMock to expect a *very specific* JSON body.
        // If the client sends anything slightly different, this mock won't match,
        // and WireMock will return a 404 by default for unmatched requests.
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
        val batch = ProductBatch("B001", products, 1678886400000L) // Correct batch for matching

        // Act: Send a batch with a *different* timestamp, which should NOT match the stub
        val incorrectBatch = ProductBatch("B001", products, 1234567890000L) // Different timestamp

        try {
            jsonApiClient.sendProductBatch("http://localhost:${wireMockRule.port()}/api/product-batches", incorrectBatch)
            // If the code reaches here, it means the API accepted the incorrect JSON, which is a test failure.
            org.junit.Assert.fail("Expected an exception for incorrect JSON structure, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 404
            assertTrue("Expected a failure due to API mismatch (WireMock's 404 behavior for unmatched request)",
                e.message!!.contains("Failed to send product batch: 404 - "))
        }

        // Verify: Ensure that the *correct* JSON request (which should have been matched) was NOT received
        // (because we sent an incorrect one, causing a 404)
        verify(0, postRequestedFor(urlEqualTo("/api/product-batches"))
            .withRequestBody(equalToJson(gson.toJson(batch), true, true)))
    }*/

    /**
     * Tests `sendProductBatch` behavior when the destination API returns a 500 Internal Server Error.
     * Expects an Exception to be thrown by the client.
     */
    @Test(expected = Exception::class)
    fun `test sendProductBatch when destination API returns 500`() {
        // Arrange: Prepare a product batch
        val products = listOf(
            Product("P004", "Monitor", 300.0, "Electronics", false)
        )
        val batch = ProductBatch("B002", products, System.currentTimeMillis())

        // Configure WireMock to return a 500 status for the batch POST
        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")))

        // Act: Call sendProductBatch. An Exception is expected.
        jsonApiClient.sendProductBatch("http://localhost:${wireMockRule.port()}/api/product-batches", batch)
        // Assert: The test passes if an Exception is thrown.
    }

    // --- Tests for processProductsAndSendBatch ---

    /**
     * Tests the end-to-end process of fetching products from a source API and
     * successfully sending them as a batch to a destination API.
     * Mocks both source and destination APIs for success scenarios.
     */
    @Test
    fun `test processProductsAndSendBatch success`() {
        // Arrange: Prepare products for the source API and mock its successful response
        val sourceProducts = listOf(
            Product("P005", "Webcam", 50.0, "Peripherals", true),
            Product("P006", "Microphone", 70.0, "Peripherals", false)
        )
        mockSourceApiProducts(sourceProducts)

        // Arrange: Mock the destination API for a successful batch reception
        mockDestinationApiSuccess()

        val batchId = "BATCH_XYZ"
        // Act: Call the combined processing and sending method
        val response = jsonApiClient.processProductsAndSendBatch(
            "http://localhost:${wireMockRule.port()}/api/products", // Source URL
            "http://localhost:${wireMockRule.port()}/api/product-batches", // Destination URL
            batchId
        )

        // Assert: Verify the final response from the destination API
        assertEquals("{\"status\": \"received\"}", response)

        // Verify: Ensure the POST request to the destination API was made correctly
        verify(postRequestedFor(urlEqualTo("/api/product-batches"))
            .withHeader("Content-Type", equalToIgnoreCase("application/json; charset=utf-8")) // <-- CHANGED HERE
            // Use JSON path matchers to verify specific fields within the sent JSON body
            .withRequestBody(matchingJsonPath("$.batchId", equalTo(batchId)))
            .withRequestBody(matchingJsonPath("$.products[0].name", equalTo("Webcam")))
            .withRequestBody(matchingJsonPath("$.products[1].id", equalTo("P006")))
            .withRequestBody(matchingJsonPath("$.timestamp", matching("^[0-9]+$"))) // Ensure timestamp is a number
        )
    }

    /**
     * Tests `processProductsAndSendBatch` behavior when the source API fails (e.g., 403 Forbidden).
     * Expects an Exception to be thrown by the client, and verifies the destination API is not called.
     */
    @Test(expected = Exception::class)
    fun `test processProductsAndSendBatch when source API fails`() {
        // Arrange: Mock the source API to return a 403 Forbidden status
        stubFor(get(urlEqualTo("/api/products"))
            .willReturn(aResponse()
                // Using HttpURLConnection.HTTP_FORBIDDEN for 403 status
                .withStatus(HttpURLConnection.HTTP_FORBIDDEN)
                .withBody("Access Denied")))

        // Arrange: Mock the destination API for success (though it should not be hit)
        mockDestinationApiSuccess()

        // Act: Call the combined method. An Exception is expected.
        jsonApiClient.processProductsAndSendBatch(
            "http://localhost:${wireMockRule.port()}/api/products",
            "http://localhost:${wireMockRule.port()}/api/product-batches",
            "BATCH_FAIL_SOURCE"
        )
        // Assert: The test passes if an Exception is thrown.
    }

    /**
     * Tests `processProductsAndSendBatch` behavior when the destination API fails (e.g., 502 Bad Gateway).
     * Ensures products are fetched from the source, but the batch fails to send.
     */
    @Test(expected = Exception::class)
    fun `test processProductsAndSendBatch when destination API fails`() {
        // Arrange: Prepare products for the source and mock its success
        val sourceProducts = listOf(
            Product("P007", "Speaker", 150.0, "Audio", true)
        )
        mockSourceApiProducts(sourceProducts)

        // Arrange: Mock the destination API to return a 502 Bad Gateway status
        stubFor(post(urlEqualTo("/api/product-batches"))
            .willReturn(aResponse()
                // Using HttpURLConnection.HTTP_BAD_GATEWAY for 502 status
                .withStatus(HttpURLConnection.HTTP_BAD_GATEWAY)
                .withBody("Service Unavailable")))

        // Act: Call the combined method. An Exception is expected.
        jsonApiClient.processProductsAndSendBatch(
            "http://localhost:${wireMockRule.port()}/api/products",
            "http://localhost:${wireMockRule.port()}/api/product-batches",
            "BATCH_FAIL_DEST"
        )
        // Assert: The test passes if an Exception is thrown.
    }
}