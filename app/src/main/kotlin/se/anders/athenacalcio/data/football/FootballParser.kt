package se.anders.athenacalcio.data.football

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.MatchStatus
import se.anders.athenacalcio.domain.StandingRow
import se.anders.athenacalcio.domain.Standings
import se.anders.athenacalcio.domain.Team
import java.time.Instant

/** Wire shapes of football-data.org v4 (only the fields used). Everything is optional: odd data must not crash (NFR-006). */
@Serializable private data class TeamDto(val id: Int? = null, val name: String? = null, val shortName: String? = null, val tla: String? = null, val crest: String? = null)
@Serializable private data class RowDto(
    val position: Int? = null, val team: TeamDto? = null, val playedGames: Int? = null, val won: Int? = null, val draw: Int? = null,
    val lost: Int? = null, val points: Int? = null, val goalsFor: Int? = null, val goalsAgainst: Int? = null, val goalDifference: Int? = null,
)
@Serializable private data class TableDto(val type: String? = null, val table: List<RowDto> = emptyList())
@Serializable private data class SeasonDto(val startDate: String? = null, val endDate: String? = null, val currentMatchday: Int? = null)
@Serializable private data class StandingsDto(val season: SeasonDto? = null, val standings: List<TableDto> = emptyList())
@Serializable private data class ScoreDto(val fullTime: GoalsDto? = null)
@Serializable private data class GoalsDto(val home: Int? = null, val away: Int? = null)
@Serializable private data class MatchDto(
    val id: Int? = null, val utcDate: String? = null, val status: String? = null,
    val homeTeam: TeamDto? = null, val awayTeam: TeamDto? = null, val score: ScoreDto? = null,
)
@Serializable private data class MatchesDto(val matches: List<MatchDto> = emptyList())

object FootballParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Parses the `TOTAL` table (the first table if there is none of that type). Throws [FootballException] of kind Parse on garbage. */
    fun standings(body: String): Standings = parsing {
        val dto = json.decodeFromString<StandingsDto>(body)
        val table = dto.standings.firstOrNull { it.type == "TOTAL" } ?: dto.standings.firstOrNull()
        Standings(
            seasonLabel = seasonLabel(dto.season?.startDate, dto.season?.endDate),
            matchday = dto.season?.currentMatchday,
            rows = table?.table.orEmpty().mapNotNull { r ->
                val team = r.team.toTeam() ?: return@mapNotNull null
                StandingRow(
                    position = r.position ?: return@mapNotNull null,
                    team = team,
                    played = r.playedGames ?: 0, won = r.won ?: 0, drawn = r.draw ?: 0, lost = r.lost ?: 0,
                    points = r.points ?: 0, goalsFor = r.goalsFor ?: 0, goalsAgainst = r.goalsAgainst ?: 0,
                    goalDifference = r.goalDifference ?: ((r.goalsFor ?: 0) - (r.goalsAgainst ?: 0)),
                )
            }.sortedBy { it.position },
        )
    }

    /** Parses a competition's matches; entries without teams or a valid date are skipped, unknown statuses become Unknown. */
    fun matches(body: String): List<Match> = parsing {
        json.decodeFromString<MatchesDto>(body).matches.mapNotNull { m ->
            Match(
                id = m.id ?: return@mapNotNull null,
                utc = runCatching { Instant.parse(m.utcDate) }.getOrNull() ?: return@mapNotNull null,
                status = status(m.status),
                home = m.homeTeam.toTeam() ?: return@mapNotNull null,
                away = m.awayTeam.toTeam() ?: return@mapNotNull null,
                homeGoals = m.score?.fullTime?.home,
                awayGoals = m.score?.fullTime?.away,
            )
        }
    }

    fun status(value: String?): MatchStatus = when (value) {
        "SCHEDULED" -> MatchStatus.Scheduled
        "TIMED" -> MatchStatus.Timed
        "IN_PLAY" -> MatchStatus.InPlay
        "PAUSED" -> MatchStatus.Paused
        "SUSPENDED" -> MatchStatus.Suspended
        "POSTPONED" -> MatchStatus.Postponed
        "CANCELLED", "CANCELED" -> MatchStatus.Cancelled
        "FINISHED" -> MatchStatus.Finished
        "AWARDED" -> MatchStatus.Awarded
        else -> MatchStatus.Unknown
    }

    /** "2026-08-23" to "2027-05-30" is "2026/27". */
    fun seasonLabel(start: String?, end: String?): String? {
        val from = start?.take(4)?.toIntOrNull() ?: return null
        val to = end?.take(4)?.toIntOrNull()
        return if (to != null && to > from) "$from/${(to % 100).toString().padStart(2, '0')}" else "$from"
    }

    private fun TeamDto?.toTeam(): Team? {
        val id = this?.id ?: return null
        val name = shortName?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: tla?.takeIf { it.isNotBlank() } ?: return null
        return Team(id, name, crest)
    }

    private inline fun <T> parsing(block: () -> T): T = try {
        block()
    } catch (e: FootballException) {
        throw e
    } catch (e: Exception) {
        throw FootballException(FootballErrorKind.Parse)
    }
}
