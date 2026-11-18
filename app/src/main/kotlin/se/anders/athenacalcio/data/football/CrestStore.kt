package se.anders.athenacalcio.data.football

import android.graphics.BitmapFactory
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import se.anders.athenacalcio.domain.Source
import se.anders.athenacalcio.domain.Team
import java.io.File

private const val FD_CREST_HOST = "crests.football-data.org"
private const val ESPN_CREST_HOST = "a.espncdn.com"

/**
 * FR-017, spec 002 FR-120: the data's own `crest` when it is an https `.png` on the source's emblem host, otherwise that
 * host's default PNG for the team id. Anything else (another host, http, svg) is ignored, so the app never fetches from
 * an address the data merely suggests.
 */
fun crestUrl(team: Team, source: Source): String {
    val host = if (source == Source.Espn) ESPN_CREST_HOST else FD_CREST_HOST
    val given = team.crest?.toHttpUrlOrNull()
    val ok = given != null && given.isHttps && given.host == host && given.encodedPath.endsWith(".png", ignoreCase = true)
    return when {
        ok -> given.toString()
        source == Source.Espn -> "https://$host/i/teamlogos/soccer/500/${team.id}.png"
        else -> "https://$host/${team.id}.png"
    }
}

/** Result of [CrestStore.updateAll]. */
data class UpdateResult(val updated: Int, val failed: Int)

/**
 * Team emblems (spec FR-017): fetched once, kept in app-private storage, decoded small, remembered in memory.
 * No key is sent. Every failure just yields null (the row then shows an empty box).
 */
class CrestStore(
    private val dir: File,
    private val http: OkHttpClient,
    private val urlFor: (Team, Source) -> String = ::crestUrl,
) {
    private val memory = LruCache<String, ImageBitmap>(80)
    private val gate = Semaphore(MAX_PARALLEL)

    /** Bumped after a manual update so emblems on screen are loaded again (spec 001 FR-038). */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /** Files and memory entries are named per [source] because the two sources' team ids are unrelated (spec 002 FR-120). */
    suspend fun load(team: Team, source: Source): ImageBitmap? {
        val name = "${source.name}-${team.id}"
        memory[name]?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            val file = File(dir, "$name.png")
            if (!file.isFile) gate.withPermit { download(urlFor(team, source), file) }
            decode(file)
        } ?: return null
        memory.put(name, bitmap)
        return bitmap
    }

    /** How many emblems are saved (spec 001 FR-038). */
    fun savedCount(): Int = savedFiles().size

    /**
     * FR-038: downloads every saved emblem again, four at a time. A file is replaced only by a valid new PNG, so a failed
     * download keeps the old emblem. Emblems are never deleted here or anywhere else (FR-037).
     */
    suspend fun updateAll(): UpdateResult = withContext(Dispatchers.IO) {
        val results = coroutineScope {
            savedFiles().map { file ->
                async {
                    val (sourceName, id) = file.nameWithoutExtension.split("-", limit = 2)
                    val team = Team(id.toInt(), "")
                    gate.withPermit { download(urlFor(team, Source.valueOf(sourceName)), file) }
                }
            }.awaitAll()
        }
        memory.evictAll()
        _revision.value += 1
        UpdateResult(updated = results.count { it }, failed = results.count { !it })
    }

    /** Files named `<Source>-<teamId>.png`; anything else in the folder is ignored. */
    private fun savedFiles(): List<File> = dir.listFiles { f -> f.isFile && Regex("(FootballData|Espn)-\\d+\\.png").matches(f.name) }?.toList().orEmpty()

    /** Writes to a temporary file first and swaps it in only when the answer is a valid, small enough PNG. */
    private fun download(url: String, target: File): Boolean {
        dir.mkdirs()
        val temp = File(dir, "${target.name}.tmp")
        return try {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful || r.header("Content-Type")?.startsWith("image/png") != true) return false
                val bytes = r.body.byteStream().use { it.readNBytes(MAX_BYTES + 1) }
                if (bytes.size > MAX_BYTES) return false
                temp.writeBytes(bytes)
                if (temp.renameTo(target)) true else { temp.delete(); false }
            }
        } catch (_: Exception) {
            temp.delete()
            false
        }
    }

    private fun decode(file: File): ImageBitmap? = try {
        if (!file.isFile) null else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= TARGET_PX && bounds.outHeight / (sample * 2) >= TARGET_PX) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                ?: run { file.delete(); null } // damaged file: forget it so it is fetched again
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val MAX_PARALLEL = 4
        const val MAX_BYTES = 300 * 1024
        const val TARGET_PX = 64
    }
}
