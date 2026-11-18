package se.anders.athenacalcio.ui.key

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.ui.KeyUiState
import se.anders.athenacalcio.ui.components.errorText
import se.anders.athenacalcio.ui.components.muted

/** FR-001: the API key, checked with one request before it is saved. Shown masked. */
@Composable
fun ApiKeyScreen(state: KeyUiState, onSave: (String) -> Unit) {
    var key by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp)) {
        Text(stringResource(R.string.key_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.key_hint), style = MaterialTheme.typography.bodyMedium, color = muted())
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = key, onValueChange = { key = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.key_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(16.dp))
        if (state.rejected) {
            Text(stringResource(R.string.key_rejected), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        state.problem?.let {
            Text(errorText(it), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = { onSave(key) }, enabled = !state.working && key.isNotBlank()) {
            Text(stringResource(if (state.working) R.string.key_working else R.string.key_save))
        }
    }
}
