package se.anders.athenacalcio

import org.junit.Assert.assertEquals
import org.junit.Test
import se.anders.athenacalcio.data.football.FootballParser
import se.anders.athenacalcio.data.football.crestUrl
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.Team

class CrestUrlTest {
    private fun url(crest: String?) = crestUrl(Team(108, "Inter", crest), Source.FootballData)
    private val derived = "https://crests.football-data.org/108.png"

    @Test fun apiPngOnTheEmblemHostIsUsed() {
        assertEquals("https://crests.football-data.org/x/108.png", url("https://crests.football-data.org/x/108.png"))
    }

    @Test fun svgFallsBackToThePngOfTheSameTeam() = assertEquals(derived, url("https://crests.football-data.org/108.svg"))

    @Test fun otherHostsAndPlainHttpAreIgnored() {
        assertEquals(derived, url("https://evil.example.com/108.png"))
        assertEquals(derived, url("http://crests.football-data.org/108.png"))
        assertEquals(derived, url("https://crests.football-data.org.evil.example.com/108.png"))
    }

    @Test fun missingOrGarbageCrestFallsBack() {
        assertEquals(derived, url(null))
        assertEquals(derived, url("not a url"))
    }

    @Test fun parserKeepsTheCrestField() {
        val body = """{"standings":[{"type":"TOTAL","table":[{"position":1,"team":{"id":108,"shortName":"Inter","crest":"https://crests.football-data.org/108.png"}}]}]}"""
        assertEquals("https://crests.football-data.org/108.png", FootballParser.standings(body).rows[0].team.crest)
    }

    @Test fun espnLogosUseTheEspnHostAndDefault() {
        val given = "https://a.espncdn.com/i/teamlogos/soccer/500/2923.png"
        assertEquals(given, crestUrl(Team(2923, "Palermo", given), Source.Espn))
        assertEquals(given, crestUrl(Team(2923, "Palermo", null), Source.Espn))
        // A football-data.org address is not accepted for an ESPN team, and the other way round.
        assertEquals(given, crestUrl(Team(2923, "Palermo", "https://crests.football-data.org/2923.png"), Source.Espn))
        assertEquals(derived, crestUrl(Team(108, "Inter", given), Source.FootballData))
    }
}
