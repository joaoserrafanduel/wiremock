package org.application

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
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
 * Test class for [HttpClient].
 */
class HttpClientTest {

    /**
     * JUnit Rule that starts and stops a WireMock server.
     * It uses `dynamicPort()` to automatically find a free port, preventing "Address already in use" errors.
     */
    @get:Rule
    val wireMockRule = WireMockRule(wireMockConfig().dynamicPort())

    /**
     * Tests a successful HTTP GET request resulting in a 200 OK status.
     */
    @Test
    fun `test HTTP GET 200 OK`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/hello"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("Hello, WireMock!")
                )
        )

        // Act: Make the HTTP GET request using SimpleHttpClient, using the dynamically assigned port
        val response = HttpClient.get("http://localhost:${wireMockRule.port()}/hello") // <--- UPDATE URL HERE!

        // Assert: Verify response
        assertEquals("Hello, WireMock!", response)
    }

    /**
     * Tests an HTTP GET request that results in a 404 Not Found status.
     */
    @Test(expected = Exception::class)
    fun `test HTTP GET 404 Not Found`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/error"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withBody("Not found!")
                )
        )

        // Act: Make the HTTP GET request.
        HttpClient.get("http://localhost:${wireMockRule.port()}/error") // <--- UPDATE URL HERE!
    }

    /**
     * Tests an HTTP GET request that results in a 400 Bad Request status.
     */
    @Test(expected = Exception::class)
    fun `test HTTP GET 400 Bad Request`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/bad-request"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withBody("Bad Request!")
                )
        )

        // Act: Make the HTTP GET request.
        HttpClient.get("http://localhost:${wireMockRule.port()}/bad-request") // <--- UPDATE URL HERE!
    }

    /**
     * Tests an HTTP GET request that results in a 500 Internal Server Error status.
     */
    @Test(expected = Exception::class)
    fun `test HTTP GET 500 Internal Server Error`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/server-error"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error!")
                )
        )

        // Act: Make the HTTP GET request.
        HttpClient.get("http://localhost:${wireMockRule.port()}/server-error") // <--- UPDATE URL HERE!
    }

    /**
     * Tests an HTTP GET request resulting in a 204 No Content status.
     */
    @Test(expected = Exception::class)
    fun `test HTTP GET 204 No Content throws exception`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/no-content"))
                .willReturn(
                    aResponse()
                        .withStatus(204)
                        .withBody("")
                )
        )

        // Act: Make the HTTP GET request.
        HttpClient.get("http://localhost:${wireMockRule.port()}/no-content") // <--- UPDATE URL HERE!
    }

    /**
     * Tests an HTTP GET request resulting in a 200 OK status with an empty body.
     */
    @Test(expected = Exception::class)
    fun `test HTTP GET 200 Empty Body throws exception`() {
        // Arrange: Configure WireMock
        stubFor(
            get(urlEqualTo("/empty-body"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("")
                )
        )

        // Act: Make the HTTP GET request.
        HttpClient.get("http://localhost:${wireMockRule.port()}/empty-body") // <--- UPDATE URL HERE!
    }

    /**
     * Tests an HTTP GET request to a host that is unreachable or does not exist.
     * This test does NOT use WireMock.
     */
    @Test
    fun `test HTTP GET with unreachable host throws IOException`() {
        // Arrange: No WireMock setup needed
        val unreachableUrl = "http://nonexistent-domain-123456.com/test"

        // Act: Attempt to make the HTTP GET request.
        val exception = assertThrows(IOException::class.java) {
            HttpClient.get(unreachableUrl)
        }

        // Assert: Verify exception type and message
        assertTrue(
            exception is UnknownHostException || exception is ConnectException,
            "Expected UnknownHostException or ConnectException, but got ${exception::class.simpleName}"
        )
        assertTrue(
            exception.message!!.contains(unreachableUrl.substringAfter("http://").substringBefore("/")) ||
                    exception.message!!.contains("Failed to connect") ||
                    exception.message!!.contains("unreachable"),
            "Exception message was: ${exception.message}"
        )
    }

    /**
     * Test to ensure the [HttpClient.getClient] method returns a non-null [OkHttpClient] instance.
     */
    @Test
    fun `test getClient returns OkHttpClient instance`() {
        // Arrange: No setup needed.

        // Act: Call the getClient method.
        val client = HttpClient.getClient()

        // Assert: Verify the returned client.
        assertNotNull(client)
    }
}