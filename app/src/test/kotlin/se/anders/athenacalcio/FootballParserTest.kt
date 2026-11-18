package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException
import se.anders.athenacalcio.data.football.FootballParser
import se.anders.athenacalcio.domain.MatchStatus

class FootballParserTest {
    @Test fun standingsUseTotalTableOrderedByPosition() {
        val s = FootballParser.standings(Fixtures.standings)
        assertEquals("2026/27", s.seasonLabel)
        assertEquals(11, s.matchday)
        assertEquals(listOf(1, 2, 3), s.rows.map { it.position }) // the home-only table and the team-less row are not used
        assertEquals("Inter", s.rows[0].team.name)
        assertEquals(23, s.rows[0].points)
        assertEquals(12, s.rows[0].goalDifference)
    }

    @Test fun missingNamesAndGoalDifferenceAreFilledIn() {
        val row = FootballParser.standings(Fixtures.standings).rows[2]
        assertEquals("No Short Name", row.team.name)
        assertEquals(0, row.goalDifference) // 10 - 10 when the API gives none
    }

    @Test fun emptyTableIsEmptyNotAnError() {
        val s = FootballParser.standings("""{"season":{"startDate":"2026-08-21"},"standings":[]}""")
        assertTrue(s.rows.isEmpty())
        assertEquals("2026", s.seasonLabel)
        assertNull(s.matchday)
    }

    @Test fun matchesSkipBrokenEntriesAndMapStatuses() {
        val m = FootballParser.matches(Fixtures.matches)
        assertEquals(listOf(1, 2, 3, 4, 5, 7), m.map { it.id }) // 6 has no valid date
        assertEquals(MatchStatus.Timed, m[2].status)
        assertEquals(MatchStatus.Unknown, m[5].status)
        assertEquals(2, m[0].homeGoals)
        assertNull(m[2].homeGoals)
    }

    @Test fun garbageIsAParseError() {
        try { FootballParser.matches("<html>"); fail() } catch (e: FootballException) { assertEquals(FootballErrorKind.Parse, e.kind) }
    }

    @Test fun seasonLabels() {
        assertEquals("2026/27", FootballParser.seasonLabel("2026-08-23", "2027-05-30"))
        assertEquals("2026", FootballParser.seasonLabel("2026-08-23", null))
        assertNull(FootballParser.seasonLabel(null, null))
    }
}
