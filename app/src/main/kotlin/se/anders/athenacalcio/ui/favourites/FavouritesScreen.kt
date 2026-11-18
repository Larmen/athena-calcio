package se.anders.athenacalcio.ui.favourites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.data.settings.sortedForDisplay
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.Standings
import se.anders.athenacalcio.domain.positionOf
import se.anders.athenacalcio.domain.MatchStatus
import se.anders.athenacalcio.domain.TeamFixtures
import se.anders.athenacalcio.domain.fixturesFor
import se.anders.athenacalcio.domain.outcomeFor
import se.anders.athenacalcio.domain.Team
import se.anders.athenacalcio.ui.Formatting
import se.anders.athenacalcio.ui.components.CrestImage
import se.anders.athenacalcio.ui.components.OutcomeBadge
import se.anders.athenacalcio.ui.FixtureKey
import se.anders.athenacalcio.ui.fixtureKey
import se.anders.athenacalcio.ui.standings.leagueLabel
import se.anders.athenacalcio.ui.components.SavedDataNote
import se.anders.athenacalcio.ui.components.errorText
import se.anders.athenacalcio.ui.components.muted

/** FR-020..025: every favourite with its last result and next fixture. */
@Composable
fun FavouritesScreen(
    favourites: List<Favourite>,
    matches: Map<FixtureKey, Loaded<List<Match>>>,
    standings: Map<Competition, Loaded<Standings>>,
    onRemove: (Favourite) -> Unit,
) {
    if (favourites.isEmpty()) {
        Text(stringResource(R.string.fav_empty), color = muted(), modifier = Modifier.fillMaxSize().padding(24.dp))
        return
    }
    val sorted = remember(favourites) { favourites.sortedForDisplay() }
    Column(Modifier.fillMaxSize()) {
        // One quiet line if any of the leagues is showing a saved copy (FR-032).
        matches.values.filterIsInstance<Loaded.Ready<*>>().firstOrNull { it.problem != null }?.let { SavedDataNote(it) }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(sorted, key = { "${it.competition}-${it.teamId}" }) { fav ->
                FavouriteCard(fav, fixtureKey(fav)?.let { matches[it] }, fav.league?.let { standings[it] }, onRemove = { onRemove(fav) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun FavouriteCard(fav: Favourite, load: Loaded<List<Match>>?, table: Loaded<Standings>?, onRemove: () -> Unit) {
    val removeDescription = stringResource(R.string.fav_remove_a11y, fav.name)
    // FR-027: the emblem sits at the left, centred against the whole card.
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CrestImage(Team(fav.teamId, fav.name), fav.league?.source, size = 48.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fav.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                fav.league?.let { league ->
                    // FR-029: "Serie A · 3rd" once the table is known, the league alone before that.
                    val position = (table as? Loaded.Ready)?.let { positionOf(fav.teamId, it.value) }
                    val label = if (position == null) leagueLabel(league) else stringResource(R.string.league_with_position, leagueLabel(league), Formatting.ordinal(position))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = muted())
                }
            }
            TextButton(onClick = onRemove, modifier = Modifier.semantics { contentDescription = removeDescription }) { Text(stringResource(R.string.fav_remove)) }
        }
        when (load) {
            null, Loaded.Loading -> Text(stringResource(R.string.fav_loading_data), color = muted(), modifier = Modifier.padding(top = 8.dp))
            is Loaded.Failed -> Text(errorText(load.kind), modifier = Modifier.padding(top = 8.dp))
            is Loaded.Ready -> {
                val fixtures = remember(load.value, fav.teamId) { fixturesFor(fav.teamId, load.value) }
                FixtureBlock(fav, fixtures)
            }
        }
        }
    }
}

@Composable
private fun FixtureBlock(fav: Favourite, fixtures: TeamFixtures) {
    Label(R.string.fav_last)
    val last = fixtures.last
    if (last == null) Text(stringResource(R.string.fav_no_result), color = muted()) else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            outcomeFor(fav.teamId, last)?.let { OutcomeBadge(it); Spacer(Modifier.width(10.dp)) }
            Text(resultLine(last), style = MaterialTheme.typography.bodyLarge)
        }
        Text(dateLine(fav.teamId, last, withTime = false), style = MaterialTheme.typography.bodySmall, color = muted())
    }
    Label(R.string.fav_next)
    val next = fixtures.next
    if (next == null) Text(stringResource(R.string.fav_no_fixture), color = muted()) else {
        if (next.status.isLive) {
            Text(
                stringResource(R.string.fixture_live_line, next.home.name, Formatting.score(next.homeGoals, next.awayGoals), next.away.name),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(stringResource(R.string.fixture_vs, next.home.name, next.away.name), style = MaterialTheme.typography.bodyLarge)
        }
        Text(dateLine(fav.teamId, next, withTime = true), style = MaterialTheme.typography.bodySmall, color = muted())
    }
}

@Composable
private fun Label(textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.labelMedium, color = muted(), modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun resultLine(match: Match): String =
    stringResource(R.string.fixture_result_line, match.home.name, Formatting.score(match.homeGoals, match.awayGoals), match.away.name)

/** "Sat 15 Nov, 18:00 · away"; a postponed match says so instead of promising a time. */
@Composable
private fun dateLine(teamId: Int, match: Match, withTime: Boolean): String {
    val side = stringResource(if (match.home.id == teamId) R.string.fixture_home else R.string.fixture_away)
    val date = Formatting.date(match.utc)
    val when_ = when {
        match.status == MatchStatus.Postponed -> stringResource(R.string.fixture_postponed)
        match.status == MatchStatus.Suspended -> stringResource(R.string.fixture_suspended)
        match.status.isLive -> side.let { return it }
        withTime -> stringResource(R.string.fixture_date_time, date, Formatting.time(match.utc))
        else -> date
    }
    return stringResource(R.string.fixture_date_ha, when_, side)
}
