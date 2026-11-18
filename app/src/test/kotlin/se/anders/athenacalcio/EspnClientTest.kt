package se.anders.athenacalcio

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import se.anders.athenacalcio.data.football.EspnClient
import se.anders.athenacalcio.data.football.EspnParser
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException

class EspnClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: EspnClient

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        client = EspnClient(OkHttpClient(), server.url("/apis/"))
    }

    @After fun tearDown() { server.close() }

    private fun kindOf(block: suspend () -> Unit): FootballErrorKind = try {
        runBlocking { block() }; fail("expected an error"); error("unreachable")
    } catch (e: FootballException) { e.kind }

    @Test fun standingsPathAndNoCredentials() {
        server.enqueue(MockResponse.Builder().body("{}").build())
        runBlocking { client.standingsBody("ita.2") }
        val req = server.takeRequest()
        assertEquals("/apis/v2/sports/soccer/ita.2/standings", req.url.encodedPath)
        assertNull(req.headers["X-Auth-Token"])
        assertNull(req.headers["Authorization"])
    }

    @Test fun scheduleMakesTwoRequestsAndWrapsThem() {
        server.enqueue(MockResponse.Builder().body("""{"events":[]}""").build())
        server.enqueue(MockResponse.Builder().body("""{"events":[]}""").build())
        val body = runBlocking { client.scheduleBody("ita.2", 2923) }
        val first = server.takeRequest(); val second = server.takeRequest()
        assertEquals("/apis/site/v2/sports/soccer/ita.2/teams/2923/schedule", first.url.encodedPath)
        assertNull(first.url.queryParameter("fixture"))
        assertEquals("true", second.url.queryParameter("fixture"))
        assertEquals("""{"past":{"events":[]},"future":{"events":[]}}""", body)
        EspnParser.schedule(body) // the wrapper is valid input for the parser
    }

    @Test fun errorsAreMapped() {
        server.enqueue(MockResponse.Builder().code(429).build())
        assertEquals(FootballErrorKind.RateLimited, kindOf { client.standingsBody("ita.2") })
        server.enqueue(MockResponse.Builder().code(500).body("x").build())
        assertEquals(FootballErrorKind.Server, kindOf { client.standingsBody("ita.2") })
        server.close()
        assertEquals(FootballErrorKind.Network, kindOf { client.standingsBody("ita.2") })
    }
}
