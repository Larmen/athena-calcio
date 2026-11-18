package se.anders.athenacalcio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.ui.Formatting
import java.time.Instant

@Composable
fun errorText(kind: FootballErrorKind): String = stringResource(
    when (kind) {
        FootballErrorKind.Network -> R.string.error_network
        FootballErrorKind.Auth -> R.string.error_auth
        FootballErrorKind.PlanRestricted -> R.string.error_plan
        FootballErrorKind.RateLimited -> R.string.error_rate
        FootballErrorKind.Server -> R.string.error_server
        FootballErrorKind.Parse -> R.string.error_parse
    },
)

/** FR-032: a quiet line when the shown data is a saved copy because the refresh failed; nothing when it is fresh. */
@Composable
fun SavedDataNote(ready: Loaded.Ready<*>) {
    val kind = ready.problem ?: return
    val clock = Formatting.time(Instant.ofEpochMilli(ready.fetchedAtMs))
    Text(
        stringResource(if (kind == FootballErrorKind.RateLimited) R.string.rate_saved else R.string.offline_saved, clock),
        style = MaterialTheme.typography.bodyMedium,
        color = muted(),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

/** Shows loading / error with Retry (unless the plan excludes the league: FR-014) / the content. */
@Composable
fun <T> LoadView(load: Loaded<T>?, onRetry: () -> Unit, content: @Composable (Loaded.Ready<T>) -> Unit) {
    when (load) {
        null, Loaded.Loading -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.loading), color = muted())
        }
        is Loaded.Failed -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(errorText(load.kind), style = MaterialTheme.typography.bodyLarge)
            if (load.kind != FootballErrorKind.PlanRestricted) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        is Loaded.Ready -> content(load)
    }
}
