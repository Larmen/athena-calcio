package se.anders.athenacalcio

/** Trimmed ESPN answers in the real shape (probed 2026-11-10, league ita.2). */
object EspnFixtures {
    private fun entry(id: Int, name: String, rank: Int?, gp: Int, w: Int, d: Int, l: Int, f: Int, a: Int, pts: Int) = """
      {"team":{"id":"$id","displayName":"$name","shortDisplayName":"$name","logos":[{"href":"https://a.espncdn.com/i/teamlogos/soccer/500/$id.png"}]},
       "stats":[{"name":"gamesPlayed","value":$gp.0},{"name":"wins","value":$w.0},{"name":"ties","value":$d.0},{"name":"losses","value":$l.0},
                {"name":"pointsFor","value":$f.0},{"name":"pointsAgainst","value":$a.0},{"name":"pointDifferential","value":${f - a}.0},{"name":"points","value":$pts.0}
                ${if (rank != null) """,{"name":"rank","value":$rank.0}""" else ""}]}"""

    val standings = """
    {"name":"Italian Serie B","season":{"year":2026,"displayName":"2026-27 Italian Serie B"},
     "children":[{"name":"Italian Serie B","standings":{"entries":[
       ${entry(106, "Bari", 2, 11, 7, 2, 2, 20, 11, 23)},
       ${entry(2923, "Palermo", 1, 11, 8, 3, 0, 25, 13, 27)},
       ${entry(120, "Vicenza", null, 11, 5, 3, 3, 14, 12, 18)},
       {"team":{"displayName":"No id"},"stats":[]}
     ]}}]}
    """.trimIndent()

    private fun team(id: Int, name: String) = """{"id":"$id","displayName":"$name","shortDisplayName":"$name","logos":[{"href":"https://a.espncdn.com/i/teamlogos/soccer/500/$id.png"}]}"""
    private fun event(id: Int, date: String, status: String, state: String, home: Pair<Int, String>, away: Pair<Int, String>, hs: String?, `as`: String?) = """
      {"id":"$id","date":"$date","competitions":[{"status":{"type":{"name":"$status","state":"$state","completed":${state == "post"}}},
        "competitors":[{"homeAway":"home","id":"${home.first}","team":${team(home.first, home.second)}${hs?.let { ""","score":{"value":$it,"displayValue":"$it"}""" } ?: ""}},
                       {"homeAway":"away","id":"${away.first}","team":${team(away.first, away.second)}${`as`?.let { ""","score":{"value":$it,"displayValue":"$it"}""" } ?: ""}}]}]}"""

    /** Palermo (2923): two played matches, a postponed one, and two upcoming ones; one event has no usable date. */
    val schedule = """
    {"past":{"events":[
       ${event(1, "2026-11-06T19:30Z", "STATUS_FULL_TIME", "post", 3346 to "Ascoli", 2923 to "Palermo", "1.0", "1.0")},
       ${event(2, "2026-11-01T18:30Z", "STATUS_FULL_TIME", "post", 2923 to "Palermo", 120 to "Vicenza", "3.0", "1.0")},
       ${event(9, "garbage", "STATUS_FULL_TIME", "post", 2923 to "Palermo", 106 to "Bari", "9.0", "0.0")}
     ]},
     "future":{"events":[
       ${event(3, "2026-11-24T19:00Z", "STATUS_SCHEDULED", "pre", 3257 to "Sudtirol", 2923 to "Palermo", null, null)},
       ${event(4, "2026-11-21T16:15Z", "STATUS_SCHEDULED", "pre", 3257 to "Catanzaro", 2923 to "Palermo", null, null)},
       ${event(5, "2026-12-05T16:15Z", "STATUS_POSTPONED", "post", 2923 to "Palermo", 106 to "Bari", null, null)},
       ${event(2, "2026-11-01T18:30Z", "STATUS_FULL_TIME", "post", 2923 to "Palermo", 120 to "Vicenza", "3.0", "1.0")}
     ]}}
    """.trimIndent()
}
