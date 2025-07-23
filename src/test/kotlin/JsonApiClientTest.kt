package org.application

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.google.gson.Gson
import org.application.data.Product
import org.application.data.ProductBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail // Import fail for explicit test failure
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.HttpURLConnection
import java.util.*

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
        stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(gson.toJson(products))
                )
        )
    }

    /**
     * Helper function to mock a successful response from the destination API's `/api/product-batches` endpoint.
     *
     * @param status The HTTP status code for the mocked response (defaults to 200 OK).
     * @param responseBody The JSON string to be returned as the API response body (defaults to "{\"status\": \"received\"}").
     */
    private fun mockDestinationApiSuccess(status: Int = 200, responseBody: String = "{\"status\": \"received\"}") {
        stubFor(
            post(urlEqualTo("/api/product-batches"))
                .willReturn(
                    aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
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
            Product(UUID.randomUUID(), "Laptop", 1200.0, "Electronics", true),
            Product(UUID.randomUUID(), "Mouse", 25.0, "Electronics", true)
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
     * Catches the expected exception and asserts its message.
     */
    @Test
    fun `test fetchProducts when source API returns 404`() {
        // Arrange: Configure WireMock to respond with a 404 Not Found status
        stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withBody("Not Found")
                )
        )

        try {
            // Act: Call the fetchProducts method. An Exception is expected here.
            jsonApiClient.fetchProducts("http://localhost:${wireMockRule.port()}/api/products")
            // If the code reaches here, it means no exception was thrown, which is a test failure.
            fail("Expected an exception for 404 response, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 404.
            assertTrue(
                "Expected exception message to contain '404 - Not Found'",
                e.message!!.contains("HTTP GET failed: 404 - Not Found")
            )
        }
    }

    /**
     * Tests the behavior of `fetchProducts` when the source API returns a 200 OK
     * status but with an empty response body.
     * Catches the expected exception and asserts its message.
     */
    @Test
    fun `test fetchProducts when source API returns empty body`() {
        // Arrange: Configure WireMock to respond with 200 OK but no body
        stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("")
                )
        ) // Empty body

        try {
            // Act: Call the fetchProducts method. An Exception is expected here.
            jsonApiClient.fetchProducts("http://localhost:${wireMockRule.port()}/api/products")
            // If the code reaches here, it means no exception was thrown, which is a test failure.
            fail("Expected an exception for empty body, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates no content.
            assertTrue(
                "Expected exception message to indicate no content",
                e.message!!.contains("No content in response body for successful request (Status: 200)")
            )
        }
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
            Product(UUID.randomUUID(), "Keyboard", 75.0, "Electronics", true)
        )
        val batch = ProductBatch(UUID.randomUUID(), products, 1678886400000L) // Example timestamp

        mockDestinationApiSuccess()

        val response =
            jsonApiClient.sendProductBatch("http://localhost:${wireMockRule.port()}/api/product-batches", batch)

        assertEquals("{\"status\": \"received\"}", response)

        // Verify that the POST request was made with the correct JSON body
        verify(
            postRequestedFor(urlEqualTo("/api/product-batches"))
                .withHeader("Content-Type", equalToIgnoreCase("application/json; charset=utf-8"))
                .withRequestBody(
                    equalToJson(
                        gson.toJson(batch),
                        true,
                        true
                    )
                ) // true, true for ignoreArrayOrder, ignoreExtraElements
        )
    }

    /**
     * Tests `sendProductBatch` behavior when the sent JSON structure does not match
     * what the destination API (mocked by WireMock) explicitly expects, leading to a 404.
     *
     * **Scenario:** A WireMock stub is explicitly defined to match a specific "incorrect"
     * JSON body (with a different timestamp) and is configured to return a 404 Not Found.
     * The `JsonApiClient` then sends this "incorrect" batch.
     * **Verification:** Asserts that the client correctly throws an exception, and that the
     * exception's message indicates a 404 error, confirming proper client-side error handling
     * for explicitly rejected (malformed) requests.
     */
    @Test
    fun `test sendProductBatch with incorrect JSON structure (explicit 404 response)`() {
        val products = listOf(
            Product(UUID.randomUUID(), "Keyboard", 75.0, "Electronics", true)
        )
        // The batch that will be sent, which is "incorrect" due to its timestamp (different from a hypothetical 'correct' one)
        val incorrectBatch = ProductBatch(UUID.randomUUID(), products, 1234567890000L)

        // Arrange: Configure WireMock to *explicitly* return a 404 for the incorrect JSON structure.
        // This stub will match the 'incorrectBatch' when it is sent.
        stubFor(
            post(urlEqualTo("/api/product-batches"))
                .withRequestBody(
                    equalToJson(
                        gson.toJson(incorrectBatch),
                        true,
                        true
                    )
                ) // Match the *incorrect* batch's JSON
                .willReturn(
                    aResponse()
                        .withStatus(404) // Explicitly return 404
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Invalid Timestamp or Data\"}")
                )
        ) // Optional: provide a custom error body

        try {
            // Act: Send the batch with the incorrect timestamp. This is expected to trigger the 404 stub.
            jsonApiClient.sendProductBatch(
                "http://localhost:${wireMockRule.port()}/api/product-batches",
                incorrectBatch
            )
            // If the code reaches here, it means the API accepted the incorrect JSON, which is a test failure.
            // JUnit's fail() method will mark the test as failed.
            fail("Expected an exception for incorrect JSON structure, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 404.
            // e.message!! asserts that the exception message is not null.
            assertTrue(
                "Expected a failure due to API mismatch (client handling 404)",
                e.message!!.contains("Failed to send product batch: 404 - ")
            ) // Check for specific error message indicating 404
        }

        // Optional: Verify that the incorrect request was indeed made exactly once.
        // This confirms the client sent the expected "bad" request.
        verify(
            1, postRequestedFor(urlEqualTo("/api/product-batches"))
                .withRequestBody(equalToJson(gson.toJson(incorrectBatch), true, true))
        )
    }

    /**
     * Tests `sendProductBatch` behavior when the destination API returns a 500 Internal Server Error.
     * Catches the expected exception and asserts its message.
     */
    @Test
    fun `test sendProductBatch when destination API returns 500`() {
        // Arrange: Prepare a product batch
        val products = listOf(
            Product(UUID.randomUUID(), "Monitor", 300.0, "Electronics", false)
        )
        val batch = ProductBatch(UUID.randomUUID(), products, System.currentTimeMillis())

        // Configure WireMock to return a 500 status for the batch POST
        stubFor(
            post(urlEqualTo("/api/product-batches"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        try {
            // Act: Call sendProductBatch. An Exception is expected.
            jsonApiClient.sendProductBatch("http://localhost:${wireMockRule.port()}/api/product-batches", batch)
            fail("Expected an exception for 500 response, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 500.
            assertTrue(
                "Expected exception message to contain '500 - Internal Server Error'",
                e.message!!.contains("Failed to send product batch: 500 - Internal Server Error")
            )
        }
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
            Product(UUID.randomUUID(), "Webcam", 50.0, "Peripherals", true),
            Product(UUID.randomUUID(), "Microphone", 70.0, "Peripherals", false)
        )
        mockSourceApiProducts(sourceProducts)

        // Arrange: Mock the destination API for a successful batch reception
        mockDestinationApiSuccess()

        val batchId = UUID.randomUUID()
        // Act: Call the combined processing and sending method
        val response = jsonApiClient.processProductsAndSendBatch(
            "http://localhost:${wireMockRule.port()}/api/products", // Source URL
            "http://localhost:${wireMockRule.port()}/api/product-batches", // Destination URL
            batchId
        )

        // Assert: Verify the final response from the destination API
        assertEquals("{\"status\": \"received\"}", response)

        // Verify: Ensure the POST request to the destination API was made correctly
        verify(
            postRequestedFor(urlEqualTo("/api/product-batches"))
                .withHeader("Content-Type", equalToIgnoreCase("application/json; charset=utf-8"))
                // Use JSON path matchers to verify specific fields within the sent JSON body
                .withRequestBody(matchingJsonPath("$.batchId", equalTo(batchId.toString())))
                .withRequestBody(matchingJsonPath("$.products[0].name", equalTo("Webcam")))
                .withRequestBody(matchingJsonPath("$.products[1].name", equalTo("Microphone")))
                .withRequestBody(matchingJsonPath("$.timestamp", matching("^[0-9]+$"))) // Ensure timestamp is a number
        )
    }

    /**
     * Tests `processProductsAndSendBatch` behavior when the source API fails (e.g., 403 Forbidden).
     * Catches the expected exception and asserts its message, and verifies the destination API is not called.
     */
    @Test
    fun `test processProductsAndSendBatch when source API fails`() {
        // Arrange: Mock the source API to return a 403 Forbidden status
        stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpURLConnection.HTTP_FORBIDDEN)
                        .withBody("Access Denied")
                )
        )

        // Arrange: Mock the destination API for success (though it should not be hit)
        mockDestinationApiSuccess()

        try {
            // Act: Call the combined method. An Exception is expected.
            jsonApiClient.processProductsAndSendBatch(
                "http://localhost:${wireMockRule.port()}/api/products",
                "http://localhost:${wireMockRule.port()}/api/product-batches",
                UUID.randomUUID()
            )
            fail("Expected an exception for source API failure, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 403.
            assertTrue(
                "Expected exception message to contain '403 - Access Denied'",
                e.message!!.contains("HTTP GET failed: 403 - Access Denied")
            )
        }

        // Verify: Ensure no POST request was made to the destination API
        verify(0, postRequestedFor(urlEqualTo("/api/product-batches")))
    }

    /**
     * Tests `processProductsAndSendBatch` behavior when the destination API fails (e.g., 502 Bad Gateway).
     * Catches the expected exception and asserts its message.
     * Ensures products are fetched from the source, but the batch fails to send.
     */
    @Test
    fun `test processProductsAndSendBatch when destination API fails`() {
        // Arrange: Prepare products for the source and mock its success
        val sourceProducts = listOf(
            Product(UUID.randomUUID(), "Speaker", 150.0, "Audio", true)
        )
        mockSourceApiProducts(sourceProducts)

        // Arrange: Mock the destination API to return a 502 Bad Gateway status
        stubFor(
            post(urlEqualTo("/api/product-batches"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpURLConnection.HTTP_BAD_GATEWAY)
                        .withBody("Service Unavailable")
                )
        )

        try {
            // Act: Call the combined method. An Exception is expected.
            jsonApiClient.processProductsAndSendBatch(
                "http://localhost:${wireMockRule.port()}/api/products",
                "http://localhost:${wireMockRule.port()}/api/product-batches",
                UUID.randomUUID()
            )
            fail("Expected an exception for destination API failure, but none was thrown.")
        } catch (e: Exception) {
            // Assert: Verify that an exception was thrown and its message indicates a 502.
            assertTrue(
                "Expected exception message to contain '502 - Service Unavailable'",
                e.message!!.contains("Failed to send product batch: 502 - Service Unavailable")
            )
        }

        // Verify: Ensure the POST request was made to the destination API
        verify(1, postRequestedFor(urlEqualTo("/api/product-batches")))
    }
}
