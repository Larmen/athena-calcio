package se.anders.athenacalcio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.Standings
import se.anders.athenacalcio.ui.favourites.FavouritesScreen
import se.anders.athenacalcio.ui.standings.StandingsScreen

/** FR-010: buttons for Favourites and the three flights, Serie C groups below it, Refresh and Settings at the top. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    tab: Tab,
    favourites: List<Favourite>,
    standings: Map<Competition, Loaded<Standings>>,
    matches: Map<FixtureKey, Loaded<List<Match>>>,
    onTab: (Tab) -> Unit,
    onToggleFavourite: (Favourite) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.main_refresh)) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.main_settings)) }
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() })
        FlowRow(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tab.entries.forEach { t -> FilterChip(selected = tab == t, onClick = { onTab(t) }, label = { Text(stringResource(t.labelRes())) }) }
        }
        val competition = tab.competition()
        if (competition == null) {
            FavouritesScreen(favourites, matches, standings, onRemove = onToggleFavourite)
        } else {
            StandingsScreen(competition, standings[competition], favourites, onRetry, onToggleFavourite)
        }
    }
}

private fun Tab.labelRes(): Int = when (this) {
    Tab.Favourites -> R.string.tab_favourites
    Tab.SerieA -> R.string.tab_serie_a
    Tab.SerieB -> R.string.tab_serie_b
}
