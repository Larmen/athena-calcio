package se.anders.athenacalcio.data.football

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import se.anders.athenacalcio.data.secrets.SecretStore
import se.anders.athenacalcio.data.settings.SettingsRepository

sealed interface KeyState {
    data object Loading : KeyState
    data object Missing : KeyState
    data object Present : KeyState
}

/**
 * The football-data.org API key (spec FR-001..004): checked before it is saved, stored in the secret store, referenced
 * from settings. The key itself is never exposed except to the client through [currentKey].
 */
class ApiKeyRepository(
    private val settings: SettingsRepository,
    private val secrets: SecretStore,
    private val api: FootballApi,
    scope: CoroutineScope,
) {
    @Volatile var currentKey: String? = null
        private set
    private val _state = MutableStateFlow<KeyState>(KeyState.Loading)
    val state: StateFlow<KeyState> = _state.asStateFlow()

    init {
        scope.launch {
            val ref = settings.settings.first().apiKeyRef
            val key = ref.takeIf { it.isNotBlank() }?.let(secrets::get)
            currentKey = key
            _state.value = if (key != null) KeyState.Present else KeyState.Missing
        }
    }

    /** Checks [key] with one request; only a key the API accepts is saved. Throws [FootballException] otherwise. */
    suspend fun save(key: String) {
        val trimmed = key.trim()
        api.checkKey(trimmed)
        val old = settings.settings.first().apiKeyRef
        if (old.isNotBlank()) secrets.delete(old)
        settings.setApiKeyRef(secrets.put(trimmed))
        currentKey = trimmed
        _state.value = KeyState.Present
    }

    /** Deletes the key (Settings → Remove API key, or the API refused it). Favourites are kept (FR-004). */
    suspend fun remove() {
        val ref = settings.settings.first().apiKeyRef
        if (ref.isNotBlank()) secrets.delete(ref)
        settings.setApiKeyRef("")
        currentKey = null
        _state.value = KeyState.Missing
    }
}
