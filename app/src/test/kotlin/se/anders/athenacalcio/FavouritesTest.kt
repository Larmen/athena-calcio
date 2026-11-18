package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.data.settings.decodeFavourites
import se.anders.athenacalcio.data.settings.encodeFavourites
import se.anders.athenacalcio.data.settings.sortedForDisplay
import se.anders.athenacalcio.data.settings.toggled
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.ui.Formatting
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class FavouritesTest {
    private val inter = Favourite(108, "Inter", "SA")
    private val bari = Favourite(7, "Bari", "SB")

    @Test fun toggleAddsThenRemoves() {
        val added = emptyList<Favourite>().toggled(inter)
        assertEquals(listOf(inter), added)
        assertTrue(added.toggled(inter).isEmpty())
    }

    @Test fun sameTeamInAnotherLeagueIsADifferentFavourite() {
        assertEquals(2, listOf(inter).toggled(inter.copy(competition = "SB")).size)
    }

    @Test fun roundTripAndDamagedJson() {
        assertEquals(listOf(inter, bari), decodeFavourites(encodeFavourites(listOf(inter, bari))))
        assertTrue(decodeFavourites("{not json").isEmpty())
        assertTrue(decodeFavourites(null).isEmpty())
        assertEquals(Competition.SerieA, inter.league)
        assertEquals(Competition.SerieB, bari.league)
        assertEquals(null, Favourite(1, "X", "ISCA").league) // Serie C is not supported yet
        assertEquals(null, Favourite(1, "X", "ZZ").league)
    }

    @Test fun displayOrderIsAlphabetical() {
        assertEquals(listOf("Bari", "Inter"), listOf(inter, bari).sortedForDisplay().map { it.name })
    }

    @Test fun formattingUsesGivenZone() {
        val at = Instant.parse("2026-11-15T18:00:00Z")
        val rome = ZoneId.of("Europe/Rome")
        assertEquals("19:00", Formatting.time(at, rome, Locale.UK))
        assertEquals("Sun 15 Nov", Formatting.date(at, rome, Locale.UK))
        assertEquals("+7", Formatting.signed(7)); assertEquals("−3", Formatting.signed(-3)); assertEquals("0", Formatting.signed(0))
        assertEquals("2–1", Formatting.score(2, 1)); assertEquals("–", Formatting.score(null, 1))
    }
}
