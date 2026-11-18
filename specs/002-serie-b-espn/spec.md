# Spec 002 – Serie B via ESPN, mixed favourites

Status: Draft · Owner: Anders · Date: 2026-11-10 · Builds on `specs/001-calcio-core`

## 1. Summary
Add **Serie B** to Athena Calcio. football-data.org has no free access to it, so Serie B comes from **ESPN's public, unofficial JSON endpoints** (no key). Favourites can mix Serie A and Serie B teams, and each favourite card shows its league again. Serie A keeps using football-data.org with the user's key (spec 001, unchanged).

## 2. Goals / Non-goals
**Goals**
- Serie B table, and last result / next fixture for Serie B favourites, in the same look as Serie A.
- One Favourites view mixing teams from both leagues, each labelled with its league.

**Non-goals**
- Serie C: nothing free found (ESPN has no Serie C; see §7). Still deferred.
- Cups, top scorers, line-ups, notifications. Any other ESPN league.
- Replacing football-data.org for Serie A.

## 3. User stories
### US1 – Serie B table (P1)
I open the Serie B tab and see the current table, with the same simple and landscape layouts and emblems as Serie A.
### US2 – Mixed favourites (P1)
I star teams in Serie A and Serie B tables. The Favourites view lists them together, alphabetically, each with its league, last result and next fixture.
### US3 – Isolated failures (P2)
If ESPN stops answering, Serie A and its favourites keep working, and the other way round.

## 4. Functional requirements
**Competitions**
- FR-101 The competition list gains **Serie B** (code `SB`, source ESPN, slug `ita.2`). Each competition has a *source* (`FootballData` or `Espn`). The tab row becomes **Favourites · Serie A · Serie B**.
- FR-102 `Favourite.competition` is the competition code, so favourites of both leagues live in one list (FR-020 unchanged). A favourite is identified by team id **and** competition.

**ESPN source** (unofficial: no documentation or stability promise)
- FR-110 Standings: `GET https://site.api.espn.com/apis/v2/sports/soccer/ita.2/standings` → `children[0].standings.entries[]`. Per entry: team id, short/display name, first logo, and the stats `rank`, `gamesPlayed`, `wins`, `ties`, `losses`, `pointsFor`, `pointsAgainst`, `pointDifferential`, `points`. Missing fields count as 0, and rank falls back to the entry order.
- FR-111 Fixtures per favourite team: `GET https://site.api.espn.com/apis/site/v2/sports/soccer/ita.2/teams/{teamId}/schedule` (played matches) and the same URL with `?fixture=true` (upcoming matches). Both are fetched and saved together. Verified 2026-11-10 for Palermo: 11 played + 27 upcoming = 38 matches. The season-wide scoreboard is **not** used (calendar-year based, capped at 100 events by default).
- FR-112 Event mapping: `date` (minute precision, `2026-11-06T19:30Z`) → instant; competitors → home/away teams and scores (a plain string in scoreboards, an object with `value` in schedules); status from `status.type` (`state` pre/in/post, `STATUS_POSTPONED`, `STATUS_CANCELED`, `STATUS_SUSPENDED`/`DELAYED`, half time = paused); unknown → ignored. Last result / next fixture use FR-023 unchanged.
- FR-113 Season label from `season.year` (2026 → "2026/27"). ESPN gives no current matchday, so only the season is shown under the title.
- FR-114 Requests only for what is on screen: one standings request when the Serie B table is open; two schedule requests per Serie B favourite when Favourites is open. Saved copy, 5-minute freshness, offline note and Refresh behave as FR-031..034. Serie B needs no key; the key screen still gates the app because Serie A needs it.
- FR-115 **Isolation.** An ESPN failure affects only Serie B content; a football-data.org failure only Serie A content. The "key refused" return to the key screen (FR-003) is triggered by football-data.org only.
- FR-116 A response of an unexpected shape shows "The answer … could not be read" (parse error), never a crash; the saved copy stays.
- FR-117 Settings → Data source lists the sources: "Serie A: football-data.org (your API key). Serie B: ESPN (unofficial, no key)". The licences screen says the same.

**Emblems**
- FR-120 Serie B emblems use the logo address ESPN gives for the team (an `https` `.png` on `a.espncdn.com`), else `https://a.espncdn.com/i/teamlogos/soccer/500/<teamId>.png`. Same rules as FR-017 otherwise; saved files and memory entries are named per source (`FootballData-108.png`, `Espn-2923.png`) so ids from two sources can never collide. Keeping and manual update follow 001 FR-037/FR-038 (files of both sources are updated by the same button). Max size per emblem is 300 KB (ESPN's 500 px PNGs are about 60–90 KB).

**Favourites view**
- FR-121 The league label returns under the team name on each favourite card (muted, e.g. "Serie A · 3rd", "Serie B · 1st"; the position part is 001 FR-029, for Serie B taken from the ESPN table). This supersedes the removal in 001 FR-021.
- FR-122 Favourites from both leagues are listed in one alphabetical list (name, then league). Fixtures for a favourite are looked up through its own source (FR-114).

## 5. Non-functional requirements
- NFR-101 HTTPS only; allowed hosts: `api.football-data.org`, `crests.football-data.org`, `site.api.espn.com`, `a.espncdn.com`. The football-data.org key header is added **only** by the football-data.org client; ESPN requests carry no credentials and no app identifiers beyond a generic OkHttp user agent.
- NFR-102 ESPN's endpoints are unofficial and may change or be restricted at any time; the app treats them as best-effort, labels them so (FR-117), and keeps them behind one interface so they can be swapped.
- NFR-103 Same accessibility, robustness (null scores, empty events, missing logos) and performance rules as spec 001.

## 6. Data
- Saved responses: `standings-SB`, `schedule-SB-<teamId>` (wrapper JSON `{"past":…,"future":…}`) in the same cache as 001 (5 MB cap).
- Favourites JSON unchanged (`competition` may now be `SB`).

## 7. Open questions
- [NEEDS CLARIFICATION] ESPN's terms of use do not clearly allow third-party apps. Acceptable for this personal app; revisit before any wider distribution.
- Serie C: no free source found (ESPN `ita.3` returns nothing; football-data.org Serie C is untested with the user's key; API-Football's free plan refuses 2026). Still deferred.
- Promotions and relegations: a favourite keeps its league code (as in 001).
