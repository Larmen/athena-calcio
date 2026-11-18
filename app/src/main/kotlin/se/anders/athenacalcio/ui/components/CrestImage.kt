package se.anders.athenacalcio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import se.anders.athenacalcio.data.football.CrestStore
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.Team

val LocalCrestStore = compositionLocalOf<CrestStore?> { null }

/** The size of the emblem box in table rows (FR-017). */
val CrestSize: Dp = 28.dp

/** FR-017: the team's emblem in a fixed box; empty while loading or when it cannot be fetched, so rows never shift. Decorative. */
@Composable
fun CrestImage(team: Team, source: Source?, modifier: Modifier = Modifier, size: Dp = CrestSize) {
    val store = LocalCrestStore.current
    val revision by (store?.revision ?: MutableStateFlow(0)).collectAsState()
    val bitmap by produceState<ImageBitmap?>(null, team.id, source, store, revision) { value = if (source == null) null else store?.load(team, source) }
    Box(modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(size)) }
    }
}
