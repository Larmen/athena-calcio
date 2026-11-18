package se.anders.athenacalcio.data.football

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.anders.athenacalcio.domain.Competition
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.Standings

/** What a screen shows for one piece of data (spec FR-031, FR-032). */
sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>

    /** [problem] is set when the refresh just failed and [value] is the saved copy from [fetchedAtMs]. */
    data class Ready<T>(val value: T, val fetchedAtMs: Long, val problem: FootballErrorKind? = null) : Loaded<T>

    /** Nothing saved and nothing could be fetched. */
    data class Failed(val kind: FootballErrorKind) : Loaded<Nothing>
}

/** FR-039: loaded without a refresh problem and younger than [freshMs]: nothing to read or fetch again. */
fun Loaded<*>.isFresh(nowMs: Long, freshMs: Long = FootballRepository.FRESH_MS): Boolean =
    this is Loaded.Ready<*> && problem == null && nowMs - fetchedAtMs < freshMs

/**
 * Standings and matches with a saved copy on disk. Network calls run one at a time (spec FR-034), never in loops,
 * and a rate-limit answer is passed on, not retried.
 */
class FootballRepository(
    private val api: FootballApi,
    private val espn: EspnApi,
    private val cache: ResponseCache,
    private val clock: () -> Long = System::currentTimeMillis,
    private val freshMs: Long = FRESH_MS,
) {
    private val network = Mutex()

    fun standings(competition: Competition, force: Boolean = false): Flow<Loaded<Standings>> = when (competition.source) {
        Source.FootballData -> load("standings-${competition.code}", force, FootballParser::standings) { api.standingsBody(competition) }
        Source.Espn -> load("standings-${competition.code}", force, EspnParser::standings) { espn.standingsBody(competition.espnSlug!!) }
    }

    /** One ESPN team's played and upcoming matches (spec 002 FR-111): ESPN has no league-wide list that is usable. */
    fun teamMatches(competition: Competition, teamId: Int, force: Boolean = false): Flow<Loaded<List<Match>>> =
        load("schedule-${competition.code}-$teamId", force, EspnParser::schedule) { espn.scheduleBody(competition.espnSlug!!, teamId) }

    fun matches(competition: Competition, force: Boolean = false): Flow<Loaded<List<Match>>> =
        load("matches-${competition.code}", force, FootballParser::matches) { api.matchesBody(competition) }

    fun clear() = cache.clear()

    /** Saved copy first (if any), then the fresh one unless the saved copy is recent; a failed refresh keeps the saved copy. */
    private fun <T> load(key: String, force: Boolean, parse: (String) -> T, fetch: suspend () -> String): Flow<Loaded<T>> = flow {
        val saved = cache.read(key)?.let { e -> runCatching { Loaded.Ready(parse(e.body), e.fetchedAtMs) }.getOrNull() }
        if (saved != null) {
            emit(saved)
            if (!force && clock() - saved.fetchedAtMs < freshMs) return@flow
        } else emit(Loaded.Loading)
        try {
            val body = network.withLock { fetch() }
            val value = parse(body)
            val now = clock()
            cache.write(key, body, now)
            emit(Loaded.Ready(value, now))
        } catch (e: FootballException) {
            emit(if (saved != null && e.kind != FootballErrorKind.Auth) saved.copy(problem = e.kind) else Loaded.Failed(e.kind))
        }
    }.flowOn(Dispatchers.Default)

    companion object { const val FRESH_MS = 5 * 60 * 1000L }
}
