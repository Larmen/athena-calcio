package se.anders.athenacalcio.data.football

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import se.anders.athenacalcio.domain.Competition
import java.io.IOException

/** What can go wrong talking to football-data.org (plan §3.3). */
enum class FootballErrorKind { Network, Auth, PlanRestricted, RateLimited, Server, Parse }

/** Carries only the [kind]: no URL, no key and no response text, so nothing secret can leak through it (constitution §4). */
class FootballException(val kind: FootballErrorKind) : Exception(kind.name)

/** The two reads the app does (spec FR-030) plus the key check (FR-001). Bodies are returned raw so they can be cached as they came. */
interface FootballApi {
    suspend fun checkKey(key: String)
    suspend fun standingsBody(competition: Competition): String
    suspend fun matchesBody(competition: Competition): String
}

/**
 * OkHttp client for `api.football-data.org/v4` (plan §3.1). The key is read per request from [keyProvider];
 * nothing here logs. [baseUrl] is replaceable for tests.
 */
class FootballClient(
    private val http: OkHttpClient,
    private val keyProvider: () -> String?,
    private val baseUrl: HttpUrl = BASE_URL.toHttpUrl(),
) : FootballApi {

    override suspend fun checkKey(key: String) { get("competitions/${Competition.SerieA.code}", key) }

    override suspend fun standingsBody(competition: Competition): String = get("competitions/${competition.code}/standings", keyProvider())

    override suspend fun matchesBody(competition: Competition): String = get("competitions/${competition.code}/matches", keyProvider())

    private suspend fun get(path: String, key: String?): String = withContext(Dispatchers.IO) {
        if (key.isNullOrBlank()) throw FootballException(FootballErrorKind.Auth)
        val request = Request.Builder().url(baseUrl.newBuilder().addPathSegments(path).build()).header("X-Auth-Token", key.trim()).build()
        try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (response.isSuccessful) body else throw FootballException(classify(response.code, body))
            }
        } catch (e: FootballException) {
            throw e
        } catch (e: IOException) {
            throw FootballException(FootballErrorKind.Network)
        }
    }

    companion object {
        const val BASE_URL = "https://api.football-data.org/v4/"

        @Serializable private data class ErrorDto(val message: String? = null)
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** HTTP status and error text to an error kind: a 403 about the plan is not a bad key (spec FR-003, FR-014). */
        fun classify(code: Int, body: String): FootballErrorKind {
            val message = runCatching { json.decodeFromString<ErrorDto>(body).message }.getOrNull().orEmpty().lowercase()
            return when {
                code == 401 -> FootballErrorKind.Auth
                code == 429 -> FootballErrorKind.RateLimited
                (code == 400 || code == 403) && "token" in message && ("invalid" in message || "missing" in message || "provide" in message) -> FootballErrorKind.Auth
                code == 403 -> FootballErrorKind.PlanRestricted
                else -> FootballErrorKind.Server
            }
        }
    }
}

/** The only way an API URL or key may be logged or shown (constitution §4): the key is replaced, the path dropped. */
fun redact(url: HttpUrl): String = "${url.scheme}://${url.host}/…"
fun redactKey(text: String, key: String): String = if (key.isBlank()) text else text.replace(key, "…")
