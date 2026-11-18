package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.anders.athenacalcio.data.football.EspnParser
import se.anders.athenacalcio.data.football.FootballParser
import se.anders.athenacalcio.domain.positionOf
import se.anders.athenacalcio.ui.Formatting

class PositionTest {
    @Test fun ordinals() {
        val expected = mapOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 10 to "10th", 11 to "11th", 12 to "12th", 13 to "13th", 20 to "20th", 21 to "21st", 22 to "22nd", 23 to "23rd", 101 to "101st", 111 to "111th")
        expected.forEach { (n, text) -> assertEquals(text, Formatting.ordinal(n)) }
    }

    @Test fun positionFromEitherSourcesTable() {
        val fd = FootballParser.standings(Fixtures.standings)
        assertEquals(1, positionOf(108, fd)) // Inter
        assertEquals(2, positionOf(109, fd)) // Juventus
        assertNull(positionOf(999, fd))
        val espn = EspnParser.standings(EspnFixtures.standings)
        assertEquals(1, positionOf(2923, espn)) // Palermo
        assertEquals(2, positionOf(106, espn))  // Bari
    }
}
