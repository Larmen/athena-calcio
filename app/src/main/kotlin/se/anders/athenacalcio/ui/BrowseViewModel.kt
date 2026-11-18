package se.anders.athenacalcio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.anders.athenacalcio.AthenaCalcioApplication
import se.anders.athenacalcio.data.football.FootballErrorKind
import se.anders.athenacalcio.data.football.FootballRepository
import se.anders.athenacalcio.data.football.Loaded
import se.anders.athenacalcio.data.football.isFresh
import se.anders.athenacalcio.data.settings.Favourite
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.Standings

/**
 * Which fixture data a favourite needs (spec 002 plan §1): `teamId == null` is the whole league's match list (football-data.org,
 * one request for all its favourites); a set `teamId` is that one team's schedule (ESPN).
 */
data class FixtureKey(val competition: Competition, val teamId: Int?)

fun fixtureKey(fav: Favourite): FixtureKey? = fav.league?.let { c -> if (c.source == Source.FootballData) FixtureKey(c, null) else FixtureKey(c, fav.teamId) }

/** What the visible screen needs: one table, or the fixture data of the favourites (plan §3.2). */
data class Target(val standings: Set<Competition> = emptySet(), val fixtures: Set<FixtureKey> = emptySet())

/** Loads and holds standings and matches for the visible screen only (constitution §3: nothing in the background). */
class BrowseViewModel(private val football: FootballRepository, private val clock: () -> Long = System::currentTimeMillis) : ViewModel() {
    private val _standings = MutableStateFlow<Map<Competition, Loaded<Standings>>>(emptyMap())
    val standings: StateFlow<Map<Competition, Loaded<Standings>>> = _standings.asStateFlow()
    private val _matches = MutableStateFlow<Map<FixtureKey, Loaded<List<Match>>>>(emptyMap())
    val matches: StateFlow<Map<FixtureKey, Loaded<List<Match>>>> = _matches.asStateFlow()

    /** Fires when the API refused the key (spec FR-003). */
    private val _rejected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rejected: SharedFlow<Unit> = _rejected.asSharedFlow()

    private val jobs = mutableMapOf<String, Job>()
    private var target = Target()
    private var lastRefreshMs = 0L

    /** Shows [next]: saved copies appear at once, stale ones are refreshed in the background (FR-031). */
    fun show(next: Target, force: Boolean = false) {
        target = next
        next.standings.forEach { c ->
            // FR-039: a table already in memory (loaded for another tab) is reused unless a refresh was asked for.
            if (!force && _standings.value[c]?.isFresh(clock()) == true) return@forEach
            launchOnce("s-${c.code}") {
                football.standings(c, force).collect { l ->
                    _standings.update { it + (c to l) }
                    if (l is Loaded.Failed && l.kind == FootballErrorKind.Auth && c.source == Source.FootballData) _rejected.tryEmit(Unit)
                }
            }
        }
        next.fixtures.forEach { key ->
            if (!force && _matches.value[key]?.isFresh(clock()) == true) return@forEach
            launchOnce("m-${key.competition.code}-${key.teamId}") {
                val flow = if (key.teamId == null) football.matches(key.competition, force) else football.teamMatches(key.competition, key.teamId, force)
                flow.collect { l ->
                    _matches.update { it + (key to l) }
                    // Only football-data.org can refuse the user's key (spec 002 FR-115).
                    if (l is Loaded.Failed && l.kind == FootballErrorKind.Auth && key.competition.source == Source.FootballData) _rejected.tryEmit(Unit)
                }
            }
        }
    }

    /** The Refresh button (FR-033). Taps closer than 15 s apart are ignored so the free plan's rate limit holds (FR-034). */
    fun refresh() {
        val now = clock()
        if (now - lastRefreshMs < MIN_REFRESH_GAP_MS) return
        lastRefreshMs = now
        show(target, force = true)
    }

    /** A failed or never-loaded piece can be tried again at once (Retry button). */
    fun retry() = show(target, force = true)

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _standings.value = emptyMap()
        _matches.value = emptyMap()
    }

    private fun launchOnce(key: String, block: suspend () -> Unit) {
        jobs[key]?.cancel()
        jobs[key] = viewModelScope.launch { block() }
    }

    companion object {
        const val MIN_REFRESH_GAP_MS = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AthenaCalcioApplication).container
                BrowseViewModel(c.football)
            }
        }
    }
}
