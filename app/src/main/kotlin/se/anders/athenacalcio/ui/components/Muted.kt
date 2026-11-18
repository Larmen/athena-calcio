package se.anders.athenacalcio.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun muted() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
