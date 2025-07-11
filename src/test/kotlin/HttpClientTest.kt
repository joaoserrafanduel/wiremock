package org.application // Correct package declaration

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class HttpClientTest {

    @get:Rule
    val wireMockRule = WireMockRule(8089) // Starts WireMock on port 8089

    @Test
    fun `test HTTP GET 200`() {
        stubFor(
            get(urlEqualTo("/hello"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("Hello, WireMock!")
                )
        )

        val response = SimpleHttpClient.get("http://localhost:8089/hello")

        assertEquals("Hello, WireMock!", response)
    }


    @Test(expected = Exception::class) // ADD THIS LINE: Tells JUnit to expect an Exception
    fun `test HTTP GET 404`() {
        stubFor(
            get(urlEqualTo("/error"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withBody("Not found!")
                )
        )

        // We expect this call to throw an Exception
        SimpleHttpClient.get("http://localhost:8089/error")
    }
}