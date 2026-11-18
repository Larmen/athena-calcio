package se.anders.athenacalcio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.anders.athenacalcio.data.football.KeyState
import se.anders.athenacalcio.ui.BrowseViewModel
import se.anders.athenacalcio.ui.MainScreen
import se.anders.athenacalcio.ui.MainViewModel
import se.anders.athenacalcio.ui.Screen
import se.anders.athenacalcio.ui.Tab
import se.anders.athenacalcio.ui.Target
import se.anders.athenacalcio.ui.fixtureKey
import se.anders.athenacalcio.ui.competition
import se.anders.athenacalcio.ui.components.LocalCrestStore
import se.anders.athenacalcio.ui.key.ApiKeyScreen
import se.anders.athenacalcio.ui.licences.LicencesScreen
import se.anders.athenacalcio.ui.settings.SettingsCallbacks
import se.anders.athenacalcio.ui.settings.SettingsScreen
import se.anders.athenacalcio.ui.theme.AthenaTheme
import se.anders.athenacalcio.ui.theme.useDarkTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val browse: BrowseViewModel by viewModels { BrowseViewModel.Factory }

    override fun onStart() {
        super.onStart()
        viewModel.refreshLauncher() // the launcher may have been installed or updated since last time
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val crests = (application as AthenaCalcioApplication).container.crests
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val launcherAvailable by viewModel.launcherAvailable.collectAsStateWithLifecycle()
            val keyState by viewModel.keyState.collectAsStateWithLifecycle()
            val keyUi by viewModel.keyUi.collectAsStateWithLifecycle()
            val stack by viewModel.stack.collectAsStateWithLifecycle()
            val emblemUpdate by viewModel.emblemUpdate.collectAsStateWithLifecycle()
            val tab by viewModel.tab.collectAsStateWithLifecycle()
            val standings by browse.standings.collectAsStateWithLifecycle()
            val matches by browse.matches.collectAsStateWithLifecycle()

            // The bar icons follow the theme the user chose, which may differ from the system's (FR-060).
            val dark = useDarkTheme(appearance.themeMode)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            // FR-003: a refused key sends the user back to the key screen.
            LaunchedEffect(Unit) { browse.rejected.collect { viewModel.keyRejected() } }

            AthenaTheme(appearance.themeMode, appearance.typeface, appearance.textScalePercent) {
              CompositionLocalProvider(LocalCrestStore provides crests) {
                when (keyState) {
                    KeyState.Loading -> Box(Modifier.fillMaxSize())
                    KeyState.Missing -> {
                        LaunchedEffect(Unit) { browse.clear() }
                        ApiKeyScreen(keyUi, viewModel::saveKey)
                    }
                    KeyState.Present -> {
                        val screen = stack.last()
                        BackHandler(enabled = stack.size > 1) { viewModel.back() }
                        when (screen) {
                            Screen.Main -> {
                                val current = tab
                                if (current != null) {
                                    // Only what is on screen is fetched (constitution §3, spec FR-030).
                                    val fixtureKeys = settings.favourites.mapNotNull(::fixtureKey).toSet()
                                    val favouriteLeagues = settings.favourites.mapNotNull { it.league }.toSet()
                                    val competition = current.competition()
                                    LaunchedEffect(current, fixtureKeys, favouriteLeagues) {
                                        browse.show(if (current == Tab.Favourites) Target(standings = favouriteLeagues, fixtures = fixtureKeys) else Target(standings = setOfNotNull(competition)))
                                    }
                                    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                        MainScreen(
                                            tab = current,
                                            favourites = settings.favourites,
                                            standings = standings,
                                            matches = matches,
                                            onTab = viewModel::selectTab,
                                            onToggleFavourite = viewModel::toggleFavourite,
                                            onRefresh = browse::refresh,
                                            onRetry = browse::retry,
                                            onSettings = { viewModel.open(Screen.Settings) },
                                        )
                                    }
                                }
                            }
                            Screen.Settings -> SettingsScreen(
                                settings, appearance, launcherAvailable, emblemUpdate,
                                SettingsCallbacks(
                                    onBack = viewModel::back,
                                    onThemeChange = viewModel::setThemeMode,
                                    onTypefaceChange = viewModel::setTypeface,
                                    onTextScaleChange = viewModel::setTextScale,
                                    onFollowLauncherChange = viewModel::setFollowLauncher,
                                    onRemoveKey = { viewModel.removeKey() },
                                    onOpenLicences = { viewModel.open(Screen.Licences) },
                                    onUpdateEmblems = viewModel::updateEmblems,
                                    onClearSavedData = { viewModel.clearSavedData(); browse.clear(); browse.retry() },
                                ),
                            )
                            Screen.Licences -> LicencesScreen(onBack = viewModel::back)
                        }
                    }
                }
              }
            }
        }
    }
}
