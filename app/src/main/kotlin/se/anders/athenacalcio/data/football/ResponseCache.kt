package se.anders.athenacalcio.data.football

import java.io.File

/** A saved API answer and when it was fetched (epoch millis). */
data class CacheEntry(val fetchedAtMs: Long, val body: String)

/**
 * Saved responses (spec FR-036): one file per request, first line = fetch time, the rest = the body exactly as the API
 * sent it. Holds no key. Total size is capped; the oldest files go first. Damaged files are ignored.
 */
class ResponseCache(private val dir: File, private val maxBytes: Long = MAX_BYTES) {
    init { dir.mkdirs() }

    fun read(key: String): CacheEntry? {
        val file = fileFor(key).takeIf { it.isFile } ?: return null
        return try {
            val text = file.readText()
            val split = text.indexOf('\n')
            if (split < 0) return null
            CacheEntry(text.substring(0, split).toLong(), text.substring(split + 1))
        } catch (_: Exception) {
            null
        }
    }

    fun write(key: String, body: String, nowMs: Long) {
        val target = fileFor(key)
        val temp = File(dir, "${target.name}.tmp")
        try {
            temp.writeText("$nowMs\n$body")
            if (!temp.renameTo(target)) { temp.delete(); return }
        } catch (_: Exception) {
            temp.delete()
            return
        }
        trim(keep = target)
    }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    private fun trim(keep: File) {
        val files = dir.listFiles { f -> f.isFile && !f.name.endsWith(".tmp") }?.sortedBy { it.lastModified() }.orEmpty().toMutableList()
        var total = files.sumOf { it.length() }
        for (f in files) {
            if (total <= maxBytes) break
            if (f == keep) continue
            total -= f.length()
            f.delete()
        }
    }

    private fun fileFor(key: String) = File(dir, key.filter { it.isLetterOrDigit() || it == '-' } + ".cache")

    companion object { const val MAX_BYTES = 5L * 1024 * 1024 }
}
