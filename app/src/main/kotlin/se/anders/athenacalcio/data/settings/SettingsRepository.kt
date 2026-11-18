package se.anders.athenacalcio.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import se.anders.athenacalcio.data.launcher.LauncherAppearance
import se.anders.athenacalcio.domain.Competition

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("settings")

/** FR-061: follow the system by default; the user can force light or dark. Names match Athena Launcher. */
enum class ThemeMode { System, Light, Dark }

/** FR-061: the same twelve typefaces as Athena Launcher, with the same names so values map one-to-one. */
enum class TypefaceChoice {
    System, Condensed, Serif, Monospace, Cursive, Casual,

    /** Bundled open-licence fonts; their licences are listed in [se.anders.athenacalcio.data.licences.Licences]. */
    Vt323, PlexMono, SpaceMono, JetBrainsMono, Inter, Atkinson,
}

/** FR-061: text size on top of the system font size (same range and default as the launcher). */
const val DEFAULT_TEXT_SCALE_PERCENT = 100
const val MIN_TEXT_SCALE_PERCENT = 80
const val MAX_TEXT_SCALE_PERCENT = 140
const val TEXT_SCALE_STEP = 10

fun clampTextScale(percent: Int): Int = percent.coerceIn(MIN_TEXT_SCALE_PERCENT, MAX_TEXT_SCALE_PERCENT)

/** FR-020: a team followed in one league. The name is kept so the list shows before (or without) any data. */
@Serializable
data class Favourite(val teamId: Int, val name: String, val competition: String) {
    val league: Competition? get() = Competition.fromCode(competition)
}

/** Adds the favourite when absent and removes it when present, matching on team and league (FR-020). */
fun List<Favourite>.toggled(fav: Favourite): List<Favourite> =
    if (any { it.teamId == fav.teamId && it.competition == fav.competition }) filterNot { it.teamId == fav.teamId && it.competition == fav.competition } else this + fav

/** Alphabetical by name, as the Favourites view lists them (FR-021). */
fun List<Favourite>.sortedForDisplay(): List<Favourite> = sortedWith(compareBy({ it.name.lowercase() }, { it.competition }))

private val favouritesJson = Json { ignoreUnknownKeys = true }
private val favouritesSerializer = ListSerializer(Favourite.serializer())

fun encodeFavourites(list: List<Favourite>): String = favouritesJson.encodeToString(favouritesSerializer, list)

/** Unreadable or damaged JSON gives an empty list rather than a crash. */
fun decodeFavourites(text: String?): List<Favourite> =
    if (text.isNullOrBlank()) emptyList() else runCatching { favouritesJson.decodeFromString(favouritesSerializer, text) }.getOrDefault(emptyList())

/** The app's own (local) settings. */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    val typeface: TypefaceChoice = TypefaceChoice.System,
    val textScalePercent: Int = DEFAULT_TEXT_SCALE_PERCENT,
    /** FR-062: take theme, typeface and text size from Athena Launcher when it is available. */
    val followLauncher: Boolean = true,
    /** FR-002: reference to the API key in the secret store. */
    val apiKeyRef: String = "",
    val favourites: List<Favourite> = emptyList(),
    /** The launcher's values as last read, so the first frame after a cold start already looks right (FR-062). */
    val launcherCache: LauncherAppearance? = null,
)

/** User preferences, stored locally in DataStore (spec §6). Unknown or damaged values fall back to defaults. */
class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsStore)

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME] } ?: ThemeMode.System,
            typeface = TypefaceChoice.entries.firstOrNull { it.name == prefs[TYPEFACE] } ?: TypefaceChoice.System,
            textScalePercent = clampTextScale(prefs[TEXT_SCALE] ?: DEFAULT_TEXT_SCALE_PERCENT),
            followLauncher = prefs[FOLLOW_LAUNCHER] ?: true,
            apiKeyRef = prefs[API_KEY_REF].orEmpty(),
            favourites = decodeFavourites(prefs[FAVOURITES]),
            launcherCache = prefs[CACHE_SCALE]?.let { scale ->
                LauncherAppearance(
                    themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[CACHE_THEME] },
                    typeface = TypefaceChoice.entries.firstOrNull { it.name == prefs[CACHE_TYPEFACE] },
                    textScalePercent = clampTextScale(scale),
                )
            },
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) { store.edit { it[THEME] = mode.name } }
    suspend fun setTypeface(choice: TypefaceChoice) { store.edit { it[TYPEFACE] = choice.name } }
    suspend fun setTextScalePercent(percent: Int) { store.edit { it[TEXT_SCALE] = clampTextScale(percent) } }
    suspend fun setFollowLauncher(value: Boolean) { store.edit { it[FOLLOW_LAUNCHER] = value } }
    suspend fun setApiKeyRef(ref: String) { store.edit { if (ref.isBlank()) it.remove(API_KEY_REF) else it[API_KEY_REF] = ref } }

    /** Adds or removes [fav] (FR-020); the whole read-modify-write happens inside one DataStore edit. */
    suspend fun toggleFavourite(fav: Favourite) {
        store.edit { it[FAVOURITES] = encodeFavourites(decodeFavourites(it[FAVOURITES]).toggled(fav)) }
    }

    suspend fun cacheLauncher(a: LauncherAppearance?) {
        store.edit {
            if (a == null || a.textScalePercent == null) {
                it.remove(CACHE_THEME); it.remove(CACHE_TYPEFACE); it.remove(CACHE_SCALE)
            } else {
                a.themeMode?.let { m -> it[CACHE_THEME] = m.name } ?: it.remove(CACHE_THEME)
                a.typeface?.let { t -> it[CACHE_TYPEFACE] = t.name } ?: it.remove(CACHE_TYPEFACE)
                it[CACHE_SCALE] = a.textScalePercent
            }
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val TYPEFACE = stringPreferencesKey("typeface")
        val TEXT_SCALE = intPreferencesKey("text_scale_percent")
        val FOLLOW_LAUNCHER = booleanPreferencesKey("follow_launcher")
        val API_KEY_REF = stringPreferencesKey("api_key_ref")
        val FAVOURITES = stringPreferencesKey("favourites")
        val CACHE_THEME = stringPreferencesKey("launcher_cache_theme")
        val CACHE_TYPEFACE = stringPreferencesKey("launcher_cache_typeface")
        val CACHE_SCALE = intPreferencesKey("launcher_cache_scale")
    }
}
