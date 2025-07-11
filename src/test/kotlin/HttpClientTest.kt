package org.application

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [SimpleHttpClient].
 *
 * This class uses WireMock to simulate HTTP server responses, allowing for
 * comprehensive testing of [SimpleHttpClient]'s behavior under various
 * successful, error, and edge-case scenarios. Each test method follows the
 * Arrange-Act-Assert (AAA) pattern for clear structure and readability.
 */
class HttpClientTest {

    /**
     * JUnit Rule that starts and stops a WireMock server on port 8089 for each test method.
     * This ensures a clean slate for network mocking in every test.
     */
    @get:Rule
    val wireMockRule = WireMockRule(8089) // Starts WireMock on port 8089

    /**
     * Tests a successful HTTP GET request resulting in a 200 OK status.
     *
     * The test stubs a WireMock endpoint to return a 200 status with a specific body.
     * It then calls [SimpleHttpClient.get] and asserts that the returned response
     * body matches the expected content.
     */
    @Test
    fun `test HTTP GET 200`() {
        // Arrange: Configure WireMock to respond with a 200 OK status and a specific body
        stubFor(
            get(urlEqualTo("/hello"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("Hello, WireMock!")
                )
        )

        // Act: Make the HTTP GET request using SimpleHttpClient
        val response = HttpClient.get("http://localhost:8089/hello")

        // Assert: Verify that the received response body matches the expected content
        assertEquals("Hello, WireMock!", response)
    }

    /**
     * Tests an HTTP GET request that results in a 404 Not Found status.
     *
     * This test expects an [Exception] to be thrown by [SimpleHttpClient.get]
     * because the client is designed to throw an exception for non-successful
     * HTTP responses (status codes outside of the 2xx range).
     */
    @Test(expected = Exception::class) // Expects an Exception due to 404 status
    fun `test HTTP GET 404`() {
        // Arrange: Configure WireMock to respond with a 404 Not Found status and a body
        stubFor(
            get(urlEqualTo("/error"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withBody("Not found!")
                )
        )

        // Act: Make the HTTP GET request. An Exception is expected to be thrown here.
        HttpClient.get("http://localhost:8089/error")

        // Assert: The test passes if the expected Exception is thrown. No further assertions needed.
    }

    /**
     * Tests an HTTP GET request that results in a 400 Bad Request status.
     *
     * Similar to the 404 test, this test expects an [Exception] to be thrown
     * due to the non-successful 400 HTTP status code.
     */
    @Test(expected = Exception::class) // Expects an Exception due to 400 status
    fun `test HTTP GET 400 Bad Request`() {
        // Arrange: Configure WireMock to respond with a 400 Bad Request status and a body
        stubFor(
            get(urlEqualTo("/bad-request"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withBody("Bad Request!")
                )
        )

        // Act: Make the HTTP GET request. An Exception is expected to be thrown here.
        HttpClient.get("http://localhost:8089/bad-request")

        // Assert: The test passes if the expected Exception is thrown. No further assertions needed.
    }

    /**
     * Tests an HTTP GET request that results in a 500 Internal Server Error status.
     *
     * This test expects an [Exception] to be thrown, as 5xx status codes also
     * fall under the non-successful response category in [SimpleHttpClient].
     */
    @Test(expected = Exception::class) // Expects an Exception due to 500 status
    fun `test HTTP GET 500 Internal Server Error`() {
        // Arrange: Configure WireMock to respond with a 500 Internal Server Error status and a body
        stubFor(
            get(urlEqualTo("/server-error"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error!")
                )
        )

        // Act: Make the HTTP GET request. An Exception is expected to be thrown here.
        HttpClient.get("http://localhost:8089/server-error")

        // Assert: The test passes if the expected Exception is thrown. No further assertions needed.
    }

    /**
     * Tests an HTTP GET request resulting in a 204 No Content status.
     *
     * Based on the [SimpleHttpClient.get] implementation, a successful status
     * (2xx) that has no content in its body (null or empty string) is considered
     * an error, and an [Exception] should be thrown. This test verifies that behavior.
     */
    @Test(expected = Exception::class) // Expects an Exception because 204 has no content, and client requires content
    fun `test HTTP GET 204 No Content throws exception`() {
        // Arrange: Configure WireMock to respond with a 204 No Content status and an empty body
        stubFor(
            get(urlEqualTo("/no-content"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withBody("") // Explicitly empty body for 204
                )
        )

        // Act: Make the HTTP GET request. An Exception is expected to be thrown here
        // due to the lack of content, as per SimpleHttpClient's logic.
        HttpClient.get("http://localhost:8089/no-content")

        // Assert: The test passes if the expected Exception is thrown. No further assertions needed.
    }

    /**
     * Tests an HTTP GET request resulting in a 200 OK status with an empty body.
     *
     * Similar to the 204 test, this verifies that [SimpleHttpClient.get] throws
     * an [Exception] even for a 200 OK status if the response body is explicitly empty.
     * This confirms the client's strict requirement for non-empty content on successful calls.
     */
    @Test(expected = Exception::class) // Expects an Exception because 200 OK with empty body
    fun `test HTTP GET 200 Empty Body throws exception`() {
        // Arrange: Configure WireMock to respond with a 200 OK status and an empty body
        stubFor(
            get(urlEqualTo("/empty-body"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("") // Explicitly empty body for 200 OK
                )
        )

        // Act: Make the HTTP GET request. An Exception is expected to be thrown here
        // due to the lack of content, as per SimpleHttpClient's logic.
        HttpClient.get("http://localhost:8089/empty-body")

        // Assert: The test passes if the expected Exception is thrown. No further assertions needed.
    }

    /**
     * Tests an HTTP GET request to a host that is unreachable or does not exist.
     *
     * This scenario simulates a network error (e.g., DNS resolution failure or
     * connection timeout) and verifies that [SimpleHttpClient.get] propagates
     * an [IOException] (or one of its more specific subclasses like
     * [UnknownHostException] or [ConnectException]).
     * WireMock is not involved in this test, as the error occurs at a lower network level.
     */
    @Test
    fun `test HTTP GET with unreachable host throws IOException`() {
        // Arrange: No WireMock stubbing needed as this tests network connectivity outside WireMock.
        // Define a URL that is highly unlikely to resolve or be reachable.
        val unreachableUrl = "http://nonexistent-domain-123456.com/test"

        // Act: Attempt to make the HTTP GET request. Use assertThrows to capture the exception.
        val exception = assertThrows(IOException::class.java) {
            HttpClient.get(unreachableUrl)
        }

        // Assert: Verify the type and content of the thrown exception.
        // It should be either an UnknownHostException (DNS failure) or a ConnectException (connection refused/timeout).
        assertTrue(
            exception is UnknownHostException || exception is ConnectException,
            "Expected UnknownHostException or ConnectException, but got ${exception::class.simpleName}"
        )
        // Verify that the exception message contains relevant information about the unreachable host or connection failure.
        assertTrue(
            exception.message!!.contains(unreachableUrl.substringAfter("http://").substringBefore("/")) || // Check for domain name in message
                    exception.message!!.contains("Failed to connect") ||
                    exception.message!!.contains("unreachable"),
            "Exception message was: ${exception.message}"
        )
    }

    /**
     * Test to ensure the [SimpleHttpClient.getClient] method returns a non-null [OkHttpClient] instance.
     * This directly calls the method, ensuring its line is covered by tests.
     */
    @Test
    fun `test getClient returns OkHttpClient instance`() {
        // Arrange: No specific setup is needed as SimpleHttpClient is a singleton object
        // and its client is initialized on first access.

        // Act: Call the getClient method to retrieve the OkHttpClient instance.
        val client = HttpClient.getClient()

        // Assert: Verify that the returned client is not null and is indeed an instance of OkHttpClient.
        assertNotNull(client)
    }
}