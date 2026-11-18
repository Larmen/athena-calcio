# Plan 002 – Serie B via ESPN

Implements `spec.md`. No new dependencies (OkHttp + kotlinx-serialization already used).

## 1. Design
- `Competition(code, source, espnSlug)`; `Source { FootballData, Espn }`.
- `EspnApi` (interface) with `standingsBody(slug)` and `scheduleBody(slug, teamId)`; `EspnClient` implements it with plain OkHttp GETs (no auth header), two requests per schedule, returned as one wrapper JSON string `{"past":<json>,"future":<json>}` so the existing one-file-per-request cache works unchanged.
- `EspnParser.standings(body)` / `schedule(body)` use `JsonElement` navigation (schedule scores come as object or string).
- `FootballRepository` takes both APIs: `standings(c)` dispatches on `c.source`; `matches(c)` stays league-wide for football-data; new `teamMatches(c, teamId)` for ESPN. Same freshness / offline behaviour via the shared `load`.
- UI keys fixture data by `FixtureKey(competition, teamId?)`: `teamId == null` = whole-league list (football-data), set = one team's schedule (ESPN). `fixtureKey(favourite)` picks the right one; `fixturesFor(teamId, matches)` is unchanged.
- `crestUrl(team, competition)` and `CrestStore.load(team, competition)`; files and memory keys prefixed by source.

## 2. Probes (2026-11-10)
- `…/ita.2/standings`: 20 entries, 2026-27, stats incl. `rank`, logos `https://a.espncdn.com/i/teamlogos/soccer/500/<id>.png` (200, `image/png`, 61–86 KB).
- `…/ita.2/teams/2923/schedule`: 11 events (all `STATUS_FULL_TIME`); `?fixture=true`: 27 (`STATUS_SCHEDULED`); event dates look like `2026-11-06T19:30Z` (no seconds), so parse with `OffsetDateTime`.
- `…/ita.2/scoreboard?dates=2026&limit=500`: 400 events mixing the 2025-26 spring and 2026-27 autumn; ranged `dates=` queries return 400. Rejected.
- ESPN `ita.3`/`ita.c` standings: no data.

## 3. Risks
Unofficial API: shape or availability may change (FR-116, NFR-102). No rate limit published; requests are only made for visible screens and shared through the 5-minute cache.
