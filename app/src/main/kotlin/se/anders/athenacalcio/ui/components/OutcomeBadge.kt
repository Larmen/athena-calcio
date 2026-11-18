package se.anders.athenacalcio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.domain.Outcome

/**
 * FR-028: the usual W/D/L form badge, drawn without colour (constitution §1): win = solid, draw = outlined,
 * loss = faint. The letter is always present, so the meaning never depends on how the shapes look.
 */
@Composable
fun OutcomeBadge(outcome: Outcome, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (letter, description) = when (outcome) {
        Outcome.Win -> stringResource(R.string.outcome_win) to stringResource(R.string.outcome_win_a11y)
        Outcome.Draw -> stringResource(R.string.outcome_draw) to stringResource(R.string.outcome_draw_a11y)
        Outcome.Loss -> stringResource(R.string.outcome_loss) to stringResource(R.string.outcome_loss_a11y)
    }
    val fill = when (outcome) { Outcome.Win -> scheme.onBackground; Outcome.Draw -> scheme.background; Outcome.Loss -> scheme.surfaceVariant }
    val ink = when (outcome) { Outcome.Win -> scheme.background; Outcome.Draw -> scheme.onBackground; Outcome.Loss -> scheme.onBackground.copy(alpha = 0.6f) }
    Surface(
        modifier = modifier.size(CrestSize).semantics { contentDescription = description },
        shape = CircleShape,
        color = fill,
        border = if (outcome == Outcome.Draw) BorderStroke(1.5.dp, scheme.onBackground) else null,
    ) {
        Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Text(letter, color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}
