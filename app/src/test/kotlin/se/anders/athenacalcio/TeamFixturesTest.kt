package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.anders.athenacalcio.data.football.FootballParser
import se.anders.athenacalcio.domain.Outcome
import se.anders.athenacalcio.domain.fixturesFor
import se.anders.athenacalcio.domain.outcomeFor

class TeamFixturesTest {
    private val matches = FootballParser.matches(Fixtures.matches)

    @Test fun lastIsLatestPlayedMatch() {
        assertEquals(2, fixturesFor(108, matches).last?.id)
    }

    @Test fun nextIsEarliestUpcomingIncludingPostponed() {
        // The postponed derby (15 Nov) comes before Napoli (22 Nov) and Roma (29 Nov).
        assertEquals(4, fixturesFor(108, matches).next?.id)
    }

    @Test fun teamWithoutMatchesHasNeither() {
        val f = fixturesFor(12345, matches)
        assertNull(f.last); assertNull(f.next)
    }

    @Test fun unknownStatusIsIgnored() {
        val f = fixturesFor(1, matches) // team 1 only has the "SOMETHING_NEW" match
        assertNull(f.last); assertNull(f.next)
    }

    @Test fun outcomeIsFromTheTeamsPointOfView() {
        val inter = matches.first { it.id == 1 } // Inter 2-1 Milan
        assertEquals(Outcome.Win, outcomeFor(108, inter))
        assertEquals(Outcome.Loss, outcomeFor(98, inter))
        assertEquals(Outcome.Draw, outcomeFor(109, matches.first { it.id == 2 }))
        assertNull(outcomeFor(999, inter))
    }
}
