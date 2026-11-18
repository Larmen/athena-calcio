package se.anders.athenacalcio.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.anders.athenacalcio.R
import se.anders.athenacalcio.data.settings.MAX_TEXT_SCALE_PERCENT
import se.anders.athenacalcio.data.settings.MIN_TEXT_SCALE_PERCENT
import se.anders.athenacalcio.data.settings.Settings
import se.anders.athenacalcio.data.settings.TEXT_SCALE_STEP
import se.anders.athenacalcio.data.settings.ThemeMode
import se.anders.athenacalcio.data.settings.TypefaceChoice
import se.anders.athenacalcio.domain.EffectiveAppearance
import se.anders.athenacalcio.ui.EmblemUpdate
import se.anders.athenacalcio.ui.components.muted
import se.anders.athenacalcio.ui.theme.fontFamily
import se.anders.athenacalcio.ui.theme.sizeAdjust

class SettingsCallbacks(
    val onBack: () -> Unit,
    val onThemeChange: (ThemeMode) -> Unit,
    val onTypefaceChange: (TypefaceChoice) -> Unit,
    val onTextScaleChange: (Int) -> Unit,
    val onFollowLauncherChange: (Boolean) -> Unit,
    val onRemoveKey: () -> Unit,
    val onClearSavedData: () -> Unit,
    val onUpdateEmblems: () -> Unit,
    val onOpenLicences: () -> Unit,
)

/** FR-064: Data source (API key), Storage, Appearance (follow launcher, theme, typeface, size), About. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings, appearance: EffectiveAppearance, launcherAvailable: Boolean, emblemUpdate: EmblemUpdate, callbacks: SettingsCallbacks) {
    Column(Modifier.fillMaxWidth().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        TextButton(onClick = callbacks.onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })

        val textSmaller = stringResource(R.string.a11y_text_smaller)
        val textLarger = stringResource(R.string.a11y_text_larger)

        Section(R.string.settings_section_data)
        Text(stringResource(R.string.settings_key_saved), style = MaterialTheme.typography.bodyMedium, color = muted())
        Text(stringResource(R.string.settings_data_sources), style = MaterialTheme.typography.bodyMedium, color = muted(), modifier = Modifier.padding(top = 4.dp))
        TextButton(onClick = callbacks.onRemoveKey) { Text(stringResource(R.string.settings_key_remove)) }

        Section(R.string.settings_section_storage)
        TextButton(onClick = callbacks.onClearSavedData) { Text(stringResource(R.string.settings_clear_cache)) }
        Text(stringResource(R.string.settings_emblems_hint), style = MaterialTheme.typography.bodyMedium, color = muted(), modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = callbacks.onUpdateEmblems, enabled = emblemUpdate != EmblemUpdate.Running) { Text(stringResource(R.string.settings_update_emblems)) }
        when (emblemUpdate) {
            EmblemUpdate.Idle -> Unit
            EmblemUpdate.Running -> Text(stringResource(R.string.emblems_updating), style = MaterialTheme.typography.bodyMedium, color = muted())
            EmblemUpdate.NoneSaved -> Text(stringResource(R.string.emblems_none), style = MaterialTheme.typography.bodyMedium, color = muted())
            is EmblemUpdate.Done -> Text(
                if (emblemUpdate.failed == 0) stringResource(R.string.emblems_updated, emblemUpdate.updated) else stringResource(R.string.emblems_updated_failed, emblemUpdate.updated, emblemUpdate.failed),
                style = MaterialTheme.typography.bodyMedium, color = muted(),
            )
        }

        Section(R.string.settings_section_appearance)
        // FR-062: follow Athena Launcher's theme, typeface and text size, or use the local ones below.
        SwitchRow(stringResource(R.string.settings_follow_launcher), settings.followLauncher, callbacks.onFollowLauncherChange)
        if (appearance.followingLauncher) {
            Text(stringResource(R.string.settings_following_note), style = MaterialTheme.typography.bodyMedium, color = muted())
            Text(
                stringResource(
                    R.string.settings_following_values,
                    stringResource(appearance.themeMode.labelRes()),
                    stringResource(appearance.typeface.labelRes()),
                    appearance.textScalePercent,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
        } else {
            if (settings.followLauncher && !launcherAvailable) Text(stringResource(R.string.settings_launcher_missing), style = MaterialTheme.typography.bodyMedium, color = muted())
            Label(R.string.settings_theme)
            ChoiceRow(stringResource(R.string.settings_theme_system), settings.themeMode == ThemeMode.System) { callbacks.onThemeChange(ThemeMode.System) }
            ChoiceRow(stringResource(R.string.settings_theme_light), settings.themeMode == ThemeMode.Light) { callbacks.onThemeChange(ThemeMode.Light) }
            ChoiceRow(stringResource(R.string.settings_theme_dark), settings.themeMode == ThemeMode.Dark) { callbacks.onThemeChange(ThemeMode.Dark) }

            Label(R.string.settings_typeface)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypefaceChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = settings.typeface == choice,
                        onClick = { callbacks.onTypefaceChange(choice) },
                        label = {
                            // Each name is drawn in its own font so the choice previews itself.
                            Text(
                                stringResource(choice.labelRes()),
                                fontFamily = choice.fontFamily(),
                                fontSize = 14.sp * (choice.sizeAdjust() / settings.typeface.sizeAdjust()),
                            )
                        },
                    )
                }
            }

            val scale = settings.textScalePercent
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_text_size, scale), modifier = Modifier.weight(1f))
                TextButton(enabled = scale > MIN_TEXT_SCALE_PERCENT, onClick = { callbacks.onTextScaleChange(scale - TEXT_SCALE_STEP) }, modifier = Modifier.semantics { contentDescription = textSmaller }) { Text("−") }
                TextButton(enabled = scale < MAX_TEXT_SCALE_PERCENT, onClick = { callbacks.onTextScaleChange(scale + TEXT_SCALE_STEP) }, modifier = Modifier.semantics { contentDescription = textLarger }) { Text("+") }
            }
        }

        Section(R.string.settings_section_about)
        TextButton(onClick = callbacks.onOpenLicences) { Text(stringResource(R.string.settings_licences)) }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

private fun TypefaceChoice.labelRes(): Int = when (this) {
    TypefaceChoice.System -> R.string.settings_typeface_system
    TypefaceChoice.Condensed -> R.string.settings_typeface_condensed
    TypefaceChoice.Serif -> R.string.settings_typeface_serif
    TypefaceChoice.Monospace -> R.string.settings_typeface_monospace
    TypefaceChoice.Cursive -> R.string.settings_typeface_cursive
    TypefaceChoice.Casual -> R.string.settings_typeface_casual
    TypefaceChoice.Vt323 -> R.string.settings_typeface_vt323
    TypefaceChoice.PlexMono -> R.string.settings_typeface_plex_mono
    TypefaceChoice.SpaceMono -> R.string.settings_typeface_space_mono
    TypefaceChoice.JetBrainsMono -> R.string.settings_typeface_jetbrains_mono
    TypefaceChoice.Inter -> R.string.settings_typeface_inter
    TypefaceChoice.Atkinson -> R.string.settings_typeface_atkinson
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Section(titleRes: Int) {
    Spacer(Modifier.height(32.dp))
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Label(textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
