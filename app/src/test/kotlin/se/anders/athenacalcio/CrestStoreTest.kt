package se.anders.athenacalcio

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import se.anders.athenacalcio.data.football.CrestStore
import java.io.File

class CrestStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var dir: File
    private lateinit var store: CrestStore

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        dir = tmp.newFolder("crests")
        // Every emblem address points at the test server, at /<teamId>.png.
        store = CrestStore(dir, OkHttpClient(), urlFor = { team, _ -> server.url("/${team.id}.png").toString() })
    }

    @After fun tearDown() { server.close() }

    private fun png(text: String) = MockResponse.Builder().addHeader("Content-Type", "image/png").body(text).build()

    @Test fun updateReplacesSavedFilesAndKeepsOldOnesWhenADownloadFails() {
        File(dir, "Espn-1.png").writeText("old-1")
        File(dir, "FootballData-2.png").writeText("old-2")
        File(dir, "notes.txt").writeText("ignored") // not an emblem file
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest): MockResponse =
                if (request.url.encodedPath == "/1.png") png("new-1") else MockResponse.Builder().code(404).build()
        }
        assertEquals(2, store.savedCount())
        val before = store.revision.value
        val r = runBlocking { store.updateAll() }
        assertEquals(1, r.updated); assertEquals(1, r.failed)
        assertEquals("new-1", File(dir, "Espn-1.png").readText())
        assertEquals("old-2", File(dir, "FootballData-2.png").readText()) // a failed update never deletes the emblem
        assertFalse(File(dir, "Espn-1.png.tmp").exists())
        assertEquals(before + 1, store.revision.value)
        assertEquals("ignored", File(dir, "notes.txt").readText())
    }

    @Test fun nonPngAndOversizedAnswersAreRejected() {
        File(dir, "Espn-3.png").writeText("old-3")
        File(dir, "Espn-4.png").writeText("old-4")
        server.dispatcher = object : mockwebserver3.Dispatcher() {
            override fun dispatch(request: mockwebserver3.RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/3.png" -> MockResponse.Builder().headers(Headers.headersOf("Content-Type", "text/html")).body("<html>").build()
                else -> png("x".repeat(400 * 1024))
            }
        }
        val r = runBlocking { store.updateAll() }
        assertEquals(0, r.updated); assertEquals(2, r.failed)
        assertEquals("old-3", File(dir, "Espn-3.png").readText())
        assertEquals("old-4", File(dir, "Espn-4.png").readText())
    }

    @Test fun nothingSavedMeansNothingToUpdate() {
        assertEquals(0, store.savedCount())
        val r = runBlocking { store.updateAll() }
        assertEquals(0, r.updated + r.failed)
    }
}
