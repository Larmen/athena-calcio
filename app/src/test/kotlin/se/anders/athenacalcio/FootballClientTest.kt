package se.anders.athenacalcio

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import se.anders.athenacalcio.data.football.FootballClient
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException
import se.anders.athenacalcio.data.football.redactKey
import se.anders.athenacalcio.domain.Competition

class FootballClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: FootballClient

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        client = FootballClient(OkHttpClient(), keyProvider = { "secret-key" }, baseUrl = server.url("/v4/"))
    }

    @After fun tearDown() { server.close() }

    private fun kindOf(block: suspend () -> Unit): FootballErrorKind = try {
        runBlocking { block() }; fail("expected an error"); error("unreachable")
    } catch (e: FootballException) { e.kind }

    @Test fun sendsKeyHeaderAndPath() {
        server.enqueue(MockResponse.Builder().body("{}").build())
        runBlocking { client.standingsBody(Competition.SerieA) }
        val req = server.takeRequest()
        assertEquals("/v4/competitions/SA/standings", req.url.encodedPath)
        assertEquals("secret-key", req.headers["X-Auth-Token"])
    }

    @Test fun keyCheckUsesGivenKeyAndSerieA() {
        server.enqueue(MockResponse.Builder().body("{}").build())
        runBlocking { client.checkKey("  typed-key ") }
        val req = server.takeRequest()
        assertEquals("/v4/competitions/SA", req.url.encodedPath)
        assertEquals("typed-key", req.headers["X-Auth-Token"])
    }

    @Test fun errorsAreMapped() {
        server.enqueue(MockResponse.Builder().code(400).body("""{"message":"Your API token is invalid.","errorCode":400}""").build())
        assertEquals(FootballErrorKind.Auth, kindOf { client.matchesBody(Competition.SerieA) })
        server.enqueue(MockResponse.Builder().code(403).body("""{"message":"The resource you are looking for is restricted and apparently not within your permissions.","errorCode":403}""").build())
        assertEquals(FootballErrorKind.PlanRestricted, kindOf { client.standingsBody(Competition.SerieA) })
        server.enqueue(MockResponse.Builder().code(429).body("""{"message":"You reached your request limit."}""").build())
        assertEquals(FootballErrorKind.RateLimited, kindOf { client.standingsBody(Competition.SerieA) })
        server.enqueue(MockResponse.Builder().code(500).body("oops").build())
        assertEquals(FootballErrorKind.Server, kindOf { client.standingsBody(Competition.SerieA) })
    }

    @Test fun unreachableServerIsNetworkError() {
        val dead = FootballClient(OkHttpClient(), { "k" }, server.url("/v4/"))
        server.close()
        assertEquals(FootballErrorKind.Network, kindOf { dead.standingsBody(Competition.SerieA) })
    }

    @Test fun missingKeyIsAuthWithoutARequest() {
        val keyless = FootballClient(OkHttpClient(), { null }, server.url("/v4/"))
        assertEquals(FootballErrorKind.Auth, kindOf { keyless.standingsBody(Competition.SerieA) })
        assertEquals(0, server.requestCount)
    }

    @Test fun keyIsRedacted() {
        assertEquals("token … used", redactKey("token secret-key used", "secret-key"))
        assertTrue(redactKey("nothing", "").contains("nothing"))
    }
}
