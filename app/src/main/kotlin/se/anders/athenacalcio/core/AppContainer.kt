package se.anders.athenacalcio.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import se.anders.athenacalcio.data.football.ApiKeyRepository
import se.anders.athenacalcio.data.football.CrestStore
import se.anders.athenacalcio.data.football.EspnClient
import se.anders.athenacalcio.data.football.FootballClient
import se.anders.athenacalcio.data.football.FootballRepository
import se.anders.athenacalcio.data.football.ResponseCache
import se.anders.athenacalcio.data.launcher.LauncherAppearanceSource
import se.anders.athenacalcio.data.secrets.KeystoreSecretStore
import se.anders.athenacalcio.data.secrets.SecretStore
import se.anders.athenacalcio.data.settings.SettingsRepository
import java.io.File

/** Manual dependency container (plan §1). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val http: OkHttpClient by lazy { OkHttpClient() }
    val settings: SettingsRepository = SettingsRepository(appContext)
    val secrets: SecretStore by lazy { KeystoreSecretStore(appContext) }
    private val client: FootballClient by lazy { FootballClient(http, keyProvider = { keys.currentKey }) }
    val keys: ApiKeyRepository by lazy { ApiKeyRepository(settings, secrets, client, scope) }
    val football: FootballRepository by lazy { FootballRepository(client, EspnClient(http), ResponseCache(File(appContext.filesDir, "football-cache"))) }
    val crests: CrestStore by lazy { CrestStore(File(appContext.filesDir, "crests"), http) }
    val launcherAppearance: LauncherAppearanceSource by lazy { LauncherAppearanceSource(appContext) }
}
