package se.anders.athenacalcio.ui.standings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import se.anders.athenacalcio.R
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.StandingRow
import se.anders.athenacalcio.domain.Standings
import se.anders.athenacalcio.ui.Formatting
import se.anders.athenacalcio.ui.components.CrestImage
import se.anders.athenacalcio.ui.components.CrestSize
import se.anders.athenacalcio.ui.components.LoadView
import se.anders.athenacalcio.ui.components.SavedDataNote
import se.anders.athenacalcio.ui.components.muted

/** FR-011..015: one league table with a star per team. */
@Composable
fun StandingsScreen(
    competition: Competition,
    load: Loaded<Standings>?,
    favourites: List<Favourite>,
    onRetry: () -> Unit,
    onToggleFavourite: (Favourite) -> Unit,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LoadView(load, onRetry) { ready ->
        val standings = ready.value
        Column(Modifier.fillMaxWidth()) {
            Text(
                leagueLabel(competition),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
            )
            val season = standings.seasonLabel
            val matchday = standings.matchday
            if (season != null && matchday != null) {
                Text(stringResource(R.string.season_matchday, season, matchday), style = MaterialTheme.typography.bodyMedium, color = muted(), modifier = Modifier.padding(horizontal = 24.dp))
            } else if (season != null) {
                Text(season, style = MaterialTheme.typography.bodyMedium, color = muted(), modifier = Modifier.padding(horizontal = 24.dp))
            }
            SavedDataNote(ready)
            if (standings.rows.isEmpty()) {
                Text(stringResource(R.string.standings_empty), color = muted(), modifier = Modifier.padding(24.dp))
            } else {
                if (landscape) DetailedHeader() else HeaderRow()
                HorizontalDivider()
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(standings.rows, key = { it.team.id }) { row ->
                        val fav = Favourite(row.team.id, row.team.name, competition.code)
                        val isFav = favourites.any { it.teamId == fav.teamId && it.competition == fav.competition }
                        if (landscape) DetailedRow(row, competition.source, isFav) { onToggleFavourite(fav) } else TableRow(row, competition.source, isFav) { onToggleFavourite(fav) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun leagueLabel(competition: Competition): String = stringResource(
    when (competition) {
        Competition.SerieA -> R.string.league_serie_a
        Competition.SerieB -> R.string.league_serie_b
    },
)

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Head(stringResource(R.string.col_position), Modifier.widthIn(min = 28.dp), TextAlign.Start)
        Spacer(Modifier.width(CrestSize + 8.dp))
        Head(stringResource(R.string.col_team), Modifier.weight(1f), TextAlign.Start)
        Head(stringResource(R.string.col_played), Modifier.widthIn(min = 32.dp), TextAlign.End)
        Head(stringResource(R.string.col_points), Modifier.widthIn(min = 40.dp), TextAlign.End)
        Row(Modifier.widthIn(min = 48.dp)) {}
    }
}

@Composable
private fun Head(text: String, modifier: Modifier, align: TextAlign) {
    Text(text, modifier = modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = muted(), textAlign = align, maxLines = 1)
}

@Composable
private fun TableRow(row: StandingRow, source: Source, isFavourite: Boolean, onToggle: () -> Unit) {
    val description = stringResource(if (isFavourite) R.string.star_remove else R.string.star_add, row.team.name)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(row.position.toString(), Modifier.widthIn(min = 28.dp), color = muted())
        CrestImage(row.team, source)
        Spacer(Modifier.width(8.dp))
        Text(row.team.name, Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.bodyLarge)
        Text(row.played.toString(), Modifier.widthIn(min = 32.dp), textAlign = TextAlign.End)
        Text(row.points.toString(), Modifier.widthIn(min = 40.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
        TextButton(onClick = onToggle, modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp).semantics { contentDescription = description }) {
            Text(if (isFavourite) "★" else "☆", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** FR-016: every column on one line, shown when the phone is held sideways. */
@Composable
private fun DetailedHeader() {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Head(stringResource(R.string.col_position), Modifier.widthIn(min = 28.dp), TextAlign.Start)
        Spacer(Modifier.width(CrestSize + 8.dp))
        Head(stringResource(R.string.col_team), Modifier.weight(1f), TextAlign.Start)
        DetailHeads.forEach { (res, width) -> Head(stringResource(res), Modifier.widthIn(min = width), TextAlign.End) }
        Row(Modifier.widthIn(min = 48.dp)) {}
    }
}

private val DetailHeads = listOf(
    R.string.col_played to 36.dp, R.string.col_won to 32.dp, R.string.col_drawn to 32.dp, R.string.col_lost to 32.dp,
    R.string.col_goals_for to 36.dp, R.string.col_goals_against to 36.dp, R.string.col_goal_diff to 44.dp, R.string.col_points to 44.dp,
)

@Composable
private fun DetailedRow(row: StandingRow, source: Source, isFavourite: Boolean, onToggle: () -> Unit) {
    val description = stringResource(if (isFavourite) R.string.star_remove else R.string.star_add, row.team.name)
    val values = listOf(
        row.played.toString(), row.won.toString(), row.drawn.toString(), row.lost.toString(),
        row.goalsFor.toString(), row.goalsAgainst.toString(), Formatting.signed(row.goalDifference), row.points.toString(),
    )
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(row.position.toString(), Modifier.widthIn(min = 28.dp), color = muted())
        CrestImage(row.team, source)
        Spacer(Modifier.width(8.dp))
        Text(row.team.name, Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        values.forEachIndexed { i, v ->
            Text(v, Modifier.widthIn(min = DetailHeads[i].second), textAlign = TextAlign.End, fontWeight = if (i == values.lastIndex) FontWeight.Bold else null, maxLines = 1)
        }
        TextButton(onClick = onToggle, modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp).semantics { contentDescription = description }) {
            Text(if (isFavourite) "★" else "☆", style = MaterialTheme.typography.titleMedium)
        }
    }
}
