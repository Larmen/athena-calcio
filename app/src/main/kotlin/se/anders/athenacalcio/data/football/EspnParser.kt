package se.anders.athenacalcio.data.football

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import se.anders.athenacalcio.domain.Match
import se.anders.athenacalcio.domain.MatchStatus
import se.anders.athenacalcio.domain.StandingRow
import se.anders.athenacalcio.domain.Standings
import se.anders.athenacalcio.domain.Team
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Reads ESPN's unofficial JSON (spec 002 FR-110..113, FR-116). Everything is looked up defensively: a field that is
 * missing or of another type counts as absent, and a body that is not JSON at all is a Parse error.
 */
object EspnParser {
    private val json = Json // strict on purpose: a non-JSON answer (an error page) must be a Parse error

    fun standings(body: String): Standings = parsing {
        val root = rootObject(body)
        val entries = root["children"].array().firstOrNull().obj()["standings"].obj()["entries"].array()
        val rows = entries.mapIndexedNotNull { index, entry ->
            val team = entry.obj()["team"].toTeam() ?: return@mapIndexedNotNull null
            val stats = entry.obj()["stats"].array().mapNotNull { s ->
                val o = s.obj(); val name = o["name"].text() ?: return@mapNotNull null
                name to o["value"].number()
            }.toMap()
            fun stat(name: String) = stats[name]?.toInt() ?: 0
            StandingRow(
                position = stats["rank"]?.toInt() ?: (index + 1),
                team = team,
                played = stat("gamesPlayed"), won = stat("wins"), drawn = stat("ties"), lost = stat("losses"),
                points = stat("points"), goalsFor = stat("pointsFor"), goalsAgainst = stat("pointsAgainst"),
                goalDifference = stats["pointDifferential"]?.toInt() ?: (stat("pointsFor") - stat("pointsAgainst")),
            )
        }.sortedBy { it.position }
        Standings(seasonLabel = seasonLabel(root["season"].obj()["year"].number()?.toInt()), matchday = null, rows = rows)
    }

    /** Parses the wrapper `{"past":…,"future":…}` of [EspnApi.scheduleBody]; events without teams or a valid date are skipped. */
    fun schedule(body: String): List<Match> = parsing {
        val root = rootObject(body)
        (root["past"].obj()["events"].array() + root["future"].obj()["events"].array()).mapNotNull { event ->
            val e = event.obj()
            val competition = e["competitions"].array().firstOrNull().obj()
            val competitors = competition["competitors"].array().map { it.obj() }
            val home = competitors.firstOrNull { it["homeAway"].text() == "home" } ?: return@mapNotNull null
            val away = competitors.firstOrNull { it["homeAway"].text() == "away" } ?: return@mapNotNull null
            Match(
                id = e["id"].text()?.toIntOrNull() ?: return@mapNotNull null,
                utc = parseInstant(e["date"].text()) ?: return@mapNotNull null,
                status = status(competition["status"].obj()["type"].obj()),
                home = home["team"].toTeam() ?: return@mapNotNull null,
                away = away["team"].toTeam() ?: return@mapNotNull null,
                homeGoals = score(home["score"]),
                awayGoals = score(away["score"]),
            )
        }.distinctBy { it.id }
    }

    /** `type.name` first for the unusual ones, then `type.state` (pre / in / post). */
    fun status(type: JsonObject): MatchStatus {
        val name = type["name"].text().orEmpty().uppercase()
        return when {
            "POSTPONED" in name -> MatchStatus.Postponed
            "CANCEL" in name || "ABANDON" in name -> MatchStatus.Cancelled
            "SUSPENDED" in name || "DELAYED" in name -> MatchStatus.Suspended
            "HALFTIME" in name -> MatchStatus.Paused
            else -> when (type["state"].text()) {
                "pre" -> MatchStatus.Scheduled
                "in" -> MatchStatus.InPlay
                "post" -> if (type["completed"].let { it is JsonPrimitive && it.contentOrNull == "true" } || "FULL_TIME" in name || "FINAL" in name) MatchStatus.Finished else MatchStatus.Unknown
                else -> if ("SCHEDULED" in name) MatchStatus.Scheduled else MatchStatus.Unknown
            }
        }
    }

    /** ESPN dates have minute precision (`2026-11-06T19:30Z`), which `Instant.parse` refuses. */
    fun parseInstant(text: String?): Instant? =
        runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull() ?: runCatching { Instant.parse(text) }.getOrNull()

    /** 2026 is "2026/27". */
    fun seasonLabel(year: Int?): String? = year?.let { "$it/${((it + 1) % 100).toString().padStart(2, '0')}" }

    /** A score is a plain string in scoreboards and an object with `value` in schedules. */
    private fun score(e: JsonElement?): Int? = when (e) {
        is JsonPrimitive -> e.contentOrNull?.toIntOrNull()
        is JsonObject -> e["value"].number()?.toInt() ?: e["displayValue"].text()?.toIntOrNull()
        else -> null
    }

    private fun JsonElement?.toTeam(): Team? {
        val o = (this as? JsonObject) ?: return null
        val id = o["id"].text()?.toIntOrNull() ?: return null
        val name = listOf("shortDisplayName", "displayName", "name", "abbreviation").firstNotNullOfOrNull { o[it].text()?.takeIf(String::isNotBlank) } ?: return null
        val logo = o["logo"].text() ?: o["logos"].array().firstOrNull().obj()["href"].text()
        return Team(id, name, logo)
    }

    /** The body must be a JSON object; anything else (an HTML error page, an empty string) is a Parse error. */
    private fun rootObject(body: String): JsonObject = json.parseToJsonElement(body) as? JsonObject ?: throw FootballException(FootballErrorKind.Parse)

    private fun JsonElement?.obj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
    private fun JsonElement?.array(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement?.number(): Double? = (this as? JsonPrimitive)?.let { it.doubleOrNull ?: it.intOrNull?.toDouble() }

    private inline fun <T> parsing(block: () -> T): T = try {
        block()
    } catch (e: FootballException) {
        throw e
    } catch (e: Exception) {
        throw FootballException(FootballErrorKind.Parse)
    }
}
