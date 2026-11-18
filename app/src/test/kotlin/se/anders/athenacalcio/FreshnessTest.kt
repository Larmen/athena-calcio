package se.anders.athenacalcio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballRepository
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.football.isFresh

class FreshnessTest {
    private val now = 10_000_000L
    private val ready = Loaded.Ready("table", fetchedAtMs = now - 60_000)

    @Test fun recentCleanCopyIsReused() = assertTrue(ready.isFresh(now))

    @Test fun oldCopyIsNot() = assertFalse(ready.copy(fetchedAtMs = now - FootballRepository.FRESH_MS - 1).isFresh(now))

    @Test fun copyShownAfterAFailedRefreshIsNotReused() = assertFalse(ready.copy(problem = FootballErrorKind.Network).isFresh(now))

    @Test fun loadingAndFailedAreNotReused() {
        assertFalse(Loaded.Loading.isFresh(now))
        assertFalse(Loaded.Failed(FootballErrorKind.Server).isFresh(now))
    }
}
