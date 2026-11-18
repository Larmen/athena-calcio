package se.anders.athenacalcio

/** Trimmed football-data.org v4 answers (same shape as the live API). */
object Fixtures {
    val standings = """
    {"filters":{"season":"2026"},"competition":{"id":2019,"code":"SA"},
     "season":{"id":2494,"startDate":"2026-08-23","endDate":"2027-05-30","currentMatchday":11,"winner":null},
     "standings":[
      {"stage":"REGULAR_SEASON","type":"HOME","table":[{"position":1,"team":{"id":99,"name":"Home Only"},"playedGames":1,"points":3}]},
      {"stage":"REGULAR_SEASON","type":"TOTAL","table":[
        {"position":2,"team":{"id":109,"name":"Juventus FC","shortName":"Juventus","tla":"JUV"},"playedGames":10,"form":null,"won":6,"draw":3,"lost":1,"points":21,"goalsFor":15,"goalsAgainst":7,"goalDifference":8},
        {"position":1,"team":{"id":108,"name":"FC Internazionale Milano","shortName":"Inter","tla":"INT"},"playedGames":10,"won":7,"draw":2,"lost":1,"points":23,"goalsFor":20,"goalsAgainst":8,"goalDifference":12},
        {"position":3,"team":{"id":5,"name":"No Short Name"},"playedGames":10,"won":5,"draw":3,"lost":2,"points":18,"goalsFor":10,"goalsAgainst":10},
        {"position":4,"team":null}
      ]}]}
    """.trimIndent()

    val matches = """
    {"matches":[
     {"id":1,"utcDate":"2026-11-01T17:00:00Z","status":"FINISHED","homeTeam":{"id":108,"shortName":"Inter"},"awayTeam":{"id":98,"shortName":"Milan"},"score":{"winner":"HOME_TEAM","fullTime":{"home":2,"away":1}}},
     {"id":2,"utcDate":"2026-11-08T14:00:00Z","status":"FINISHED","homeTeam":{"id":109,"shortName":"Juventus"},"awayTeam":{"id":108,"shortName":"Inter"},"score":{"fullTime":{"home":1,"away":1}}},
     {"id":3,"utcDate":"2026-11-22T19:45:00Z","status":"TIMED","homeTeam":{"id":108,"shortName":"Inter"},"awayTeam":{"id":113,"shortName":"Napoli"},"score":{"fullTime":{"home":null,"away":null}}},
     {"id":4,"utcDate":"2026-11-15T19:45:00Z","status":"POSTPONED","homeTeam":{"id":98,"shortName":"Milan"},"awayTeam":{"id":108,"shortName":"Inter"},"score":{"fullTime":{}}},
     {"id":5,"utcDate":"2026-11-29T19:45:00Z","status":"SCHEDULED","homeTeam":{"id":108,"shortName":"Inter"},"awayTeam":{"id":100,"shortName":"Roma"}},
     {"id":6,"utcDate":"not a date","status":"FINISHED","homeTeam":{"id":1,"shortName":"X"},"awayTeam":{"id":2,"shortName":"Y"}},
     {"id":7,"utcDate":"2026-11-02T14:00:00Z","status":"SOMETHING_NEW","homeTeam":{"id":1,"shortName":"X"},"awayTeam":{"id":2,"shortName":"Y"}}
    ]}
    """.trimIndent()
}
