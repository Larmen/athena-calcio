package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import se.anders.athenacalcio.data.football.EspnParser
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException
import se.anders.athenacalcio.domain.MatchStatus
import se.anders.athenacalcio.domain.fixturesFor
import java.time.Instant

class EspnParserTest {
    @Test fun standingsAreOrderedByRankWithStatsAndLogo() {
        val s = EspnParser.standings(EspnFixtures.standings)
        assertEquals("2026/27", s.seasonLabel)
        assertNull(s.matchday)
        assertEquals(listOf("Palermo", "Bari", "Vicenza"), s.rows.map { it.team.name }) // the entry without an id is skipped
        val p = s.rows[0]
        assertEquals(1, p.position); assertEquals(11, p.played); assertEquals(8, p.won); assertEquals(3, p.drawn); assertEquals(0, p.lost)
        assertEquals(25, p.goalsFor); assertEquals(13, p.goalsAgainst); assertEquals(12, p.goalDifference); assertEquals(27, p.points)
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/2923.png", p.team.crest)
    }

    @Test fun missingRankFallsBackToEntryOrder() {
        // Vicenza has no rank stat and is the third entry that survived... but it sorts after the ranked ones.
        assertEquals(3, EspnParser.standings(EspnFixtures.standings).rows.last().position)
    }

    @Test fun scheduleMergesPastAndFutureAndSkipsBadEvents() {
        val m = EspnParser.schedule(EspnFixtures.schedule)
        assertEquals(listOf(1, 2, 3, 4, 5), m.map { it.id }) // 9 has no valid date; event 2 appears in both lists once
        assertEquals(Instant.parse("2026-11-06T19:30:00Z"), m[0].utc)
        assertEquals(MatchStatus.Finished, m[0].status)
        assertEquals(1, m[0].homeGoals); assertEquals(1, m[0].awayGoals)
        assertEquals(MatchStatus.Scheduled, m[2].status)
        assertNull(m[2].homeGoals)
        assertEquals(MatchStatus.Postponed, m[4].status)
    }

    @Test fun lastAndNextForTheFavourite() {
        val f = fixturesFor(2923, EspnParser.schedule(EspnFixtures.schedule))
        assertEquals(1, f.last?.id)  // 6 Nov is later than 1 Nov
        assertEquals(4, f.next?.id)  // 21 Nov is the earliest upcoming one
    }

    @Test fun statusMapping() {
        fun st(name: String, state: String, completed: Boolean = false) =
            EspnParser.status(kotlinx.serialization.json.Json.parseToJsonElement("""{"name":"$name","state":"$state","completed":$completed}""") as kotlinx.serialization.json.JsonObject)
        assertEquals(MatchStatus.InPlay, st("STATUS_FIRST_HALF", "in"))
        assertEquals(MatchStatus.Paused, st("STATUS_HALFTIME", "in"))
        assertEquals(MatchStatus.Cancelled, st("STATUS_CANCELED", "post"))
        assertEquals(MatchStatus.Suspended, st("STATUS_DELAYED", "pre"))
        assertEquals(MatchStatus.Finished, st("STATUS_FINAL_PEN", "post", completed = true))
        assertEquals(MatchStatus.Unknown, st("STATUS_WHATEVER", "post"))
    }

    @Test fun datesWithoutSecondsAndSeasonLabels() {
        assertEquals(Instant.parse("2026-11-06T19:30:00Z"), EspnParser.parseInstant("2026-11-06T19:30Z"))
        assertEquals(Instant.parse("2026-11-06T19:30:15Z"), EspnParser.parseInstant("2026-11-06T19:30:15Z"))
        assertNull(EspnParser.parseInstant("nope"))
        assertEquals("2026/27", EspnParser.seasonLabel(2026)); assertEquals("2099/00", EspnParser.seasonLabel(2099)); assertNull(EspnParser.seasonLabel(null))
    }

    @Test fun unexpectedShapeIsEmptyAndGarbageIsAParseError() {
        assertTrue(EspnParser.standings("{}").rows.isEmpty())
        assertTrue(EspnParser.schedule("""{"past":{},"future":{}}""").isEmpty())
        try { EspnParser.standings("<html>"); fail() } catch (e: FootballException) { assertEquals(FootballErrorKind.Parse, e.kind) }
    }
}
