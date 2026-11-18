package se.anders.athenacalcio.domain

/** A favourite's last result and next fixture (spec FR-022, FR-023). */
data class TeamFixtures(val last: Match?, val next: Match?)

/**
 * FR-023: last = the team's latest played league match that has a score; next = its earliest not-yet-played one
 * (live matches count as next, postponed ones keep their old date). Cancelled and unknown statuses are ignored.
 */
fun fixturesFor(teamId: Int, matches: List<Match>): TeamFixtures {
    val mine = matches.filter { it.home.id == teamId || it.away.id == teamId }
    return TeamFixtures(
        last = mine.filter { it.status.isPlayed && it.homeGoals != null && it.awayGoals != null }.maxByOrNull { it.utc },
        next = mine.filter { it.status.isUpcoming }.minByOrNull { it.utc },
    )
}

enum class Outcome { Win, Draw, Loss }

/** The result of [match] from [teamId]'s point of view, or null when there is no score or the team did not play. */
fun outcomeFor(teamId: Int, match: Match): Outcome? {
    val h = match.homeGoals ?: return null
    val a = match.awayGoals ?: return null
    val mine = when (teamId) { match.home.id -> h - a; match.away.id -> a - h; else -> return null }
    return when { mine > 0 -> Outcome.Win; mine < 0 -> Outcome.Loss; else -> Outcome.Draw }
}
