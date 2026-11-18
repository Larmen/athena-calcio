package se.anders.athenacalcio.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Dates and times in the phone's time zone and locale (spec FR-035). Zone and locale are parameters so tests can pin them. */
object Formatting {
    fun date(at: Instant, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE d MMM", locale).withZone(zone).format(at)

    fun time(at: Instant, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone).format(at)

    /** English ordinal for a table position: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, 22nd (spec 001 FR-029). */
    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }

    /** "1–0" with a real en dash; a missing score shows as "–". */
    fun score(home: Int?, away: Int?): String = if (home == null || away == null) "–" else "$home–$away"

    /** Goal difference with an explicit sign: +7, 0, −3. */
    fun signed(value: Int): String = when {
        value > 0 -> "+$value"
        value < 0 -> "−${-value}"
        else -> "0"
    }
}
