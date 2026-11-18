package se.anders.athenacalcio

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import se.anders.athenacalcio.data.football.EspnApi
import se.anders.athenacalcio.data.football.FootballApi
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException
import se.anders.athenacalcio.data.football.FootballRepository
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.football.ResponseCache
import se.anders.athenacalcio.domain.Competition
import java.io.File

private class FakeApi : FootballApi {
    var calls = 0
    var failure: FootballErrorKind? = null
    override suspend fun checkKey(key: String) {}
    override suspend fun standingsBody(competition: Competition): String { calls++; failure?.let { throw FootballException(it) }; return Fixtures.standings }
    override suspend fun matchesBody(competition: Competition): String { calls++; failure?.let { throw FootballException(it) }; return Fixtures.matches }
}

private class FakeEspn : EspnApi {
    var calls = 0
    var failure: FootballErrorKind? = null
    override suspend fun standingsBody(slug: String): String { calls++; failure?.let { throw FootballException(it) }; return EspnFixtures.standings }
    override suspend fun scheduleBody(slug: String, teamId: Int): String { calls++; failure?.let { throw FootballException(it) }; return EspnFixtures.schedule }
}

class FootballRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val api = FakeApi()
    private val espn = FakeEspn()
    private var now = 1_000_000L
    private lateinit var repo: FootballRepository
    private lateinit var cache: ResponseCache

    @Before fun setUp() {
        cache = ResponseCache(tmp.newFolder("c"))
        repo = FootballRepository(api, espn, cache, clock = { now })
    }

    private fun load(force: Boolean = false) = runBlocking { repo.standings(Competition.SerieA, force).toList() }

    @Test fun firstLoadFetchesAndSaves() {
        val r = load()
        assertTrue(r[0] is Loaded.Loading)
        assertEquals(1000000L, (r[1] as Loaded.Ready).fetchedAtMs)
        assertEquals(1, api.calls)
    }

    @Test fun freshSavedCopyIsShownWithoutFetching() {
        load()
        now += 60_000
        val r = load()
        assertEquals(1, r.size)
        assertEquals(1, api.calls)
    }

    @Test fun staleSavedCopyIsShownThenReplaced() {
        load()
        now += FootballRepository.FRESH_MS + 1
        val r = load()
        assertEquals(2, r.size)
        assertEquals(1_000_000L, (r[0] as Loaded.Ready).fetchedAtMs)
        assertEquals(now, (r[1] as Loaded.Ready).fetchedAtMs)
        assertEquals(2, api.calls)
    }

    @Test fun forceRefetchesEvenWhenFresh() {
        load(); load(force = true)
        assertEquals(2, api.calls)
    }

    @Test fun failedRefreshKeepsSavedCopyWithProblem() {
        load()
        now += FootballRepository.FRESH_MS + 1
        api.failure = FootballErrorKind.Network
        val last = load().last() as Loaded.Ready
        assertEquals(FootballErrorKind.Network, last.problem)
        assertEquals(3, last.value.rows.size)
    }

    @Test fun nothingSavedAndOfflineFails() {
        api.failure = FootballErrorKind.RateLimited
        assertEquals(Loaded.Failed(FootballErrorKind.RateLimited), load().last())
    }

    @Test fun refusedKeyFailsEvenWithSavedCopy() {
        load()
        now += FootballRepository.FRESH_MS + 1
        api.failure = FootballErrorKind.Auth
        assertEquals(Loaded.Failed(FootballErrorKind.Auth), load().last())
    }

    @Test fun damagedSavedFileIsIgnored() {
        load()
        File(tmp.root, "c").listFiles()!!.forEach { it.writeText("garbage") }
        val r = load()
        assertTrue(r.first() is Loaded.Loading)
        assertTrue(r.last() is Loaded.Ready)
    }

    @Test fun cacheIsCappedAndClearable() {
        val small = ResponseCache(tmp.newFolder("s"), maxBytes = 100)
        small.write("a", "x".repeat(80), 1)
        small.write("b", "y".repeat(80), 2)
        assertTrue(small.read("b") != null)
        assertTrue(small.read("a") == null)
        small.clear()
        assertTrue(small.read("b") == null)
    }
}

class EspnRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val espn = FakeEspn()
    private var now = 5_000_000L
    private lateinit var repo: FootballRepository

    @Before fun setUp() { repo = FootballRepository(FakeApi(), espn, ResponseCache(tmp.newFolder("e")), clock = { now }) }

    @Test fun serieBStandingsComeFromEspnAndAreSaved() {
        val r = runBlocking { repo.standings(Competition.SerieB).toList() }
        assertEquals(3, (r.last() as Loaded.Ready).value.rows.size)
        runBlocking { repo.standings(Competition.SerieB).toList() }
        assertEquals(1, espn.calls) // the second read is fresh and comes from disk
    }

    @Test fun teamScheduleIsCachedPerTeamAndKeepsSavedCopyOffline() {
        runBlocking { repo.teamMatches(Competition.SerieB, 2923).toList() }
        now += FootballRepository.FRESH_MS + 1
        espn.failure = FootballErrorKind.Network
        val last = runBlocking { repo.teamMatches(Competition.SerieB, 2923).toList() }.last() as Loaded.Ready
        assertEquals(FootballErrorKind.Network, last.problem)
        assertEquals(5, last.value.size)
        // Another team has its own cache entry, so with ESPN down it fails instead of showing Palermo's data.
        assertEquals(Loaded.Failed(FootballErrorKind.Network), runBlocking { repo.teamMatches(Competition.SerieB, 106).toList() }.last())
    }
}
