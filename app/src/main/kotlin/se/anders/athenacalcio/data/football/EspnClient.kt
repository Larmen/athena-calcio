package se.anders.athenacalcio.data.football

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** ESPN's public, unofficial endpoints (spec 002 FR-110, FR-111). Bodies are returned raw so they can be cached as they came. */
interface EspnApi {
    suspend fun standingsBody(slug: String): String

    /** The team's played and upcoming matches as one wrapper `{"past":…,"future":…}` (spec 002 FR-111). */
    suspend fun scheduleBody(slug: String, teamId: Int): String
}

/** No key, no account: ESPN requests carry no credentials (spec 002 NFR-101). [baseUrl] is replaceable for tests. */
class EspnClient(
    private val http: OkHttpClient,
    private val baseUrl: HttpUrl = BASE_URL.toHttpUrl(),
) : EspnApi {

    override suspend fun standingsBody(slug: String): String =
        get(baseUrl.newBuilder().addPathSegments("v2/sports/soccer/$slug/standings").build())

    override suspend fun scheduleBody(slug: String, teamId: Int): String {
        val base = "site/v2/sports/soccer/$slug/teams/$teamId/schedule"
        val past = get(baseUrl.newBuilder().addPathSegments(base).build())
        val future = get(baseUrl.newBuilder().addPathSegments(base).addQueryParameter("fixture", "true").build())
        return """{"past":$past,"future":$future}"""
    }

    private suspend fun get(url: HttpUrl): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val body = r.body.string()
                when {
                    r.isSuccessful -> body
                    r.code == 429 -> throw FootballException(FootballErrorKind.RateLimited)
                    else -> throw FootballException(FootballErrorKind.Server)
                }
            }
        } catch (e: FootballException) {
            throw e
        } catch (e: IOException) {
            throw FootballException(FootballErrorKind.Network)
        }
    }

    companion object { const val BASE_URL = "https://site.api.espn.com/apis/" }
}
