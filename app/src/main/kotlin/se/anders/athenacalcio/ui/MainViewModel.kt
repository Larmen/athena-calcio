package se.anders.athenacalcio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.anders.athenacalcio.AthenaCalcioApplication
import se.anders.athenacalcio.data.football.ApiKeyRepository
import se.anders.athenacalcio.data.football.CrestStore
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballException
import se.anders.athenacalcio.data.football.FootballRepository
import se.anders.athenacalcio.data.football.KeyState
import se.anders.athenacalcio.data.launcher.AppearanceSource
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.data.settings.Settings
import se.anders.athenacalcio.data.settings.SettingsRepository
import se.anders.athenacalcio.data.settings.ThemeMode
import se.anders.athenacalcio.data.settings.TypefaceChoice
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.EffectiveAppearance
import se.anders.athenacalcio.domain.effectiveAppearance

/** The rows of buttons on the main screen (spec FR-010). */
enum class Tab { Favourites, SerieA, SerieB }

fun Tab.competition(): Competition? = when (this) {
    Tab.Favourites -> null
    Tab.SerieA -> Competition.SerieA
    Tab.SerieB -> Competition.SerieB
}

/** What the Settings line under "Update team emblems" says (spec 001 FR-038). */
sealed interface EmblemUpdate {
    data object Idle : EmblemUpdate
    data object Running : EmblemUpdate
    data object NoneSaved : EmblemUpdate
    data class Done(val updated: Int, val failed: Int) : EmblemUpdate
}

sealed interface Screen {
    data object Main : Screen
    data object Settings : Screen
    data object Licences : Screen
}

/** What the API-key form shows after an attempt. [rejected] means a saved key was refused later (spec FR-003). */
data class KeyUiState(val working: Boolean = false, val problem: FootballErrorKind? = null, val rejected: Boolean = false)

/** Navigation, API key, favourites and appearance (single activity, plan §3.7). */
class MainViewModel(
    private val settingsRepo: SettingsRepository,
    private val launcher: AppearanceSource,
    private val keys: ApiKeyRepository,
    private val football: FootballRepository,
    private val crests: CrestStore,
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Theme, typeface and text size actually in use: the launcher's while following it, else the local ones (FR-062). */
    val appearance: StateFlow<EffectiveAppearance> = combine(settingsRepo.settings, launcher.appearance) { settings, live -> effectiveAppearance(settings, live ?: settings.launcherCache) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, effectiveAppearance(Settings(), null))

    val launcherAvailable: StateFlow<Boolean> = launcher.unavailable.map { !it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val keyState: StateFlow<KeyState> = keys.state

    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Main))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    /** Null until the saved favourites are known: the first tab is Favourites if there are any, else Serie A. */
    private val _tab = MutableStateFlow<Tab?>(null)
    val tab: StateFlow<Tab?> = _tab.asStateFlow()

    private val _keyUi = MutableStateFlow(KeyUiState())
    val keyUi: StateFlow<KeyUiState> = _keyUi.asStateFlow()

    init {
        viewModelScope.launch { _tab.value = if (settingsRepo.settings.first().favourites.isEmpty()) Tab.SerieA else Tab.Favourites }
        // Remember the launcher's latest values; forget them once the launcher is known to be gone.
        viewModelScope.launch { launcher.appearance.collect { if (it != null) settingsRepo.cacheLauncher(it) } }
        viewModelScope.launch { launcher.unavailable.collect { if (it) settingsRepo.cacheLauncher(null) } }
    }

    fun refreshLauncher() = launcher.refresh()

    fun selectTab(tab: Tab) { _tab.value = tab }

    fun open(screen: Screen) = _stack.update { it + screen }
    fun back() = _stack.update { if (it.size > 1) it.dropLast(1) else it }

    fun toggleFavourite(fav: Favourite) = viewModelScope.launch { settingsRepo.toggleFavourite(fav) }

    /** FR-001: check the key with one request; it is saved only if the API accepts it. */
    fun saveKey(key: String) {
        if (_keyUi.value.working) return
        _keyUi.value = KeyUiState(working = true)
        viewModelScope.launch {
            _keyUi.value = try {
                keys.save(key)
                _stack.value = listOf(Screen.Main)
                KeyUiState()
            } catch (e: FootballException) {
                KeyUiState(problem = e.kind)
            }
        }
    }

    /** FR-004: Settings → Remove API key. Saved data goes with it; favourites stay. */
    fun removeKey() = viewModelScope.launch {
        keys.remove()
        football.clear()
        _stack.value = listOf(Screen.Main)
        _keyUi.value = KeyUiState()
    }

    /** FR-003: the API stopped accepting the saved key. */
    fun keyRejected() = viewModelScope.launch {
        if (keys.state.value != KeyState.Present) return@launch
        keys.remove()
        football.clear()
        _stack.value = listOf(Screen.Main)
        _keyUi.value = KeyUiState(rejected = true)
    }

    /** FR-036: Settings → Clear saved data. */
    fun clearSavedData() = football.clear()

    private val _emblemUpdate = MutableStateFlow<EmblemUpdate>(EmblemUpdate.Idle)
    val emblemUpdate: StateFlow<EmblemUpdate> = _emblemUpdate.asStateFlow()

    /** FR-038: Settings → Update team emblems. Ignored while an update is already running. */
    fun updateEmblems() {
        if (_emblemUpdate.value == EmblemUpdate.Running) return
        _emblemUpdate.value = EmblemUpdate.Running
        viewModelScope.launch {
            _emblemUpdate.value = if (crests.savedCount() == 0) EmblemUpdate.NoneSaved else crests.updateAll().let { EmblemUpdate.Done(it.updated, it.failed) }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setTypeface(choice: TypefaceChoice) = viewModelScope.launch { settingsRepo.setTypeface(choice) }
    fun setTextScale(percent: Int) = viewModelScope.launch { settingsRepo.setTextScalePercent(percent) }
    fun setFollowLauncher(value: Boolean) = viewModelScope.launch { settingsRepo.setFollowLauncher(value) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AthenaCalcioApplication).container
                MainViewModel(c.settings, c.launcherAppearance, c.keys, c.football, c.crests)
            }
        }
    }
}
