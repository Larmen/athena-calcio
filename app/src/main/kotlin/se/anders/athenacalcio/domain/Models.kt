package se.anders.athenacalcio.domain

import java.time.Instant

/** [crest] is the emblem address as the API gave it (maybe svg or another host); use [se.anders.athenacalcio.data.football.crestUrl] to draw it. */
data class Team(val id: Int, val name: String, val crest: String? = null)

/** One line of a league table (spec FR-011). */
data class StandingRow(
    val position: Int,
    val team: Team,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
)

/** [seasonLabel] is for example "2026/27"; [matchday] is the league's current matchday when the API says it (FR-012). */
data class Standings(val seasonLabel: String?, val matchday: Int?, val rows: List<StandingRow>)

/** The team's position in [standings], or null when the table does not list it (spec 001 FR-029). */
fun positionOf(teamId: Int, standings: Standings): Int? = standings.rows.firstOrNull { it.team.id == teamId }?.position

enum class MatchStatus {
    Scheduled, Timed, InPlay, Paused, Suspended, Postponed, Cancelled, Finished, Awarded, Unknown;

    val isPlayed: Boolean get() = this == Finished || this == Awarded
    val isLive: Boolean get() = this == InPlay || this == Paused

    /** FR-023: not played yet (including live, postponed and suspended); cancelled and unknown ones are left out. */
    val isUpcoming: Boolean get() = this == Scheduled || this == Timed || this == InPlay || this == Paused || this == Suspended || this == Postponed
}

data class Match(
    val id: Int,
    val utc: Instant,
    val status: MatchStatus,
    val home: Team,
    val away: Team,
    /** Current or final score (full time); null while the API gives none. */
    val homeGoals: Int?,
    val awayGoals: Int?,
)
