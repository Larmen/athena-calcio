package se.anders.athenacalcio.domain

/** Where a league's data comes from (spec 002 FR-101). Their team ids are unrelated to each other. */
enum class Source { FootballData, Espn }

/**
 * The leagues the app reads (spec 001 FR-010, spec 002 FR-101). [code] is stored in favourites; [espnSlug] is set for
 * ESPN leagues. Serie C is deferred (spec 002 §7): adding it means adding entries here.
 */
enum class Competition(val code: String, val source: Source, val espnSlug: String? = null) {
    SerieA("SA", Source.FootballData),
    SerieB("SB", Source.Espn, espnSlug = "ita.2");

    companion object {
        fun fromCode(code: String?): Competition? = entries.firstOrNull { it.code == code }
    }
}
