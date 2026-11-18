# Spec 001 – Calcio Core

Status: Draft · Owner: Anders · Date: 2026-11-10

## 1. Summary
A minimal Android app that shows the **current standings of Serie A** (the top Italian flight) and lets the user mark **favourite teams**, for which it shows the **last result** and the **next fixture**. Data comes from football-data.org with the user's own API key (the same source as `../mappa-del-calcio`). Serie B and Serie C were originally in scope but are **deferred** (see §7): no free API was found that covers them for the current season, so they will be added in a later spec. The look (monochrome, typeface, text size, light/dark) matches the other Athena apps and follows Athena Launcher's settings.

## 2. Goals / Non-goals
**Goals**
- Open the app, see where my teams stand and when they play next, with the least that works.
- Same look and appearance configuration as the other Athena apps.

**Non-goals (v1)**
- Cups (Coppa Italia, Supercoppa) and European fixtures: a favourite's last/next is **league matches only** (FR-023).
- **Serie B** is added in `specs/002-serie-b-espn`; **Serie C** stays deferred (needs a paid plan or another data source). The code keeps the competition list and the favourites' league code extensible.
- Top scorers, line-ups, match details, head-to-head, historical seasons, other countries.
- Push notifications, live-score updates in the background, widgets, calendar export.
- SVG team emblems (only PNG emblems are used, FR-017), and emblems anywhere but the standings table and the Favourites view.
- Multiple API keys, accounts, cloud sync. Android 15 or older. Play Store distribution.

## 3. User stories
### US1 – Connect (P1)
I paste my football-data.org API key once; the app checks it and remembers it (encrypted).
- Acceptance: a wrong key → clear message, nothing saved. Removing the key in Settings deletes it and everything saved.

### US2 – Standings (P1)
I open Serie A and see the table: position, team, played, goal difference, points, plus wins/draws/losses and goals for/against.

### US3 – Favourites (P1)
In any table I tap the star next to a team to make it a favourite. A Favourites view lists my teams, each with its **last result** and **next fixture**.
- Acceptance: a favourite that played yesterday shows that score and W/D/L; its next fixture shows opponent, home/away and local date and time; a live match is shown as "Live" with the current score.

### US4 – Works offline (P2)
Without a connection I still see the last standings and fixtures I loaded, with a quiet note saying so.

### US5 – Appearance like the launcher (P2)
Theme, typeface and text size follow Athena Launcher; local settings apply when the launcher is absent.

## 4. Functional requirements
**Data source key**
- FR-001 First-run screen asks for the football-data.org API key (masked field). It is checked with one request (`GET /v4/competitions/SA`) before anything is saved; errors are specific (invalid key, no network, rate limited, server error).
- FR-002 The key is stored in the encrypted secret store (reference in DataStore). It is never logged, never shown beyond "API key saved", never exported.
- FR-003 If the API later refuses the key (HTTP 401, or 400/403 with an "invalid token" message), the app returns to the key screen with a message. A 403 that says the plan does not include a competition is **not** a key problem: see FR-014.
- FR-004 Settings → "Remove API key" deletes the key, the favourites' cached data and returns to the key screen. (Favourites themselves are kept.)

**Leagues & standings**
- FR-010 Main screen has a row of plain buttons: **Favourites · Serie A** (selected one is marked). Competition code: `SA`. Further leagues are added by extending the `Competition` list (later spec).
- FR-011 Standings table (`/v4/competitions/{code}/standings`, the `TOTAL` table, in the order given): header row; in portrait (the simple view) each row is one line: position, team (short name), played, points, and the star. W, D, L, GF, GA and goal difference appear only in the detailed landscape view (FR-016).
- FR-016 **Detailed table in landscape.** When the phone is held sideways the table shows every column on one line (the only place W, D, L, GF, GA and GD are shown): position, team, P, W, D, L, GF, GA, GD, Pts (and the star). In portrait the compact layout of FR-011 is kept. Rotating switches between them at once without reloading, losing the scroll position of the table, or refetching data; the favourites star works in both.
- FR-017 **Team emblems.** Each table row (portrait and landscape) shows the team's emblem, as a PNG, between the position number and the team name, about 28 dp square, in its original colours. The image address is the `crest` the API gives for the team when it is an `https` `.png` on `crests.football-data.org`; otherwise `https://crests.football-data.org/<teamId>.png` is used. Any other address (other host, http, svg) is ignored. While an emblem loads, or if it cannot be fetched, an empty box of the same size keeps the rows aligned (no layout jump, no error text). Emblems are fetched once, kept in app-private storage (max 300 KB each, at most four downloads at once), decoded at the size drawn, and shown again offline. They are decorative: no content description. The key is never sent to the image host.
- FR-012 Under the title: season and matchday, e.g. "2026/27 · matchday 11". Current season only.
- FR-013 Each row has a star button (★ favourite / ☆ not), with a content description naming the team.
- FR-014 If the API answers 403 "not part of your plan" for a competition, that league shows "Your football-data.org plan does not include this league." with no retry loop (kept for when more leagues are added).
- FR-015 Neutral styling: no coloured zones (champions league/relegation); a thin divider between rows.

**Favourites**
- FR-020 A favourite is a team **in a league** (team id, name, competition code). Toggled by the star in a standings row, or by "Remove" in the Favourites view.
- FR-021 The Favourites view lists favourites alphabetically by name. *(Spec 002 FR-121 restores the league label and FR-122 mixes leagues.)*
- FR-022 For each favourite: **Last result** — outcome badge (FR-028), the match as `home score away`, then date and H/A on a muted line; **Next fixture** — opponent, H/A, local weekday, date and time (or "Live – 1–0" while in play/paused, "Postponed" when postponed). With none: "No result yet" / "No fixture scheduled".
- FR-023 Last result = the favourite's latest `FINISHED` or `AWARDED` league match; next fixture = its earliest not-yet-finished league match (`SCHEDULED`, `TIMED`, `IN_PLAY`, `PAUSED`, `SUSPENDED`, `POSTPONED`); `CANCELLED` is ignored. League matches only (non-goal: cups).
- FR-026 **Start screen.** The app opens on Favourites when at least one favourite is saved, otherwise on Serie A. Favourites (and the API key) survive app updates; only uninstalling or clearing the app's storage removes them.
- FR-024 Favourites are stored locally in DataStore with the team name, so the list is present offline and before the first response.
- FR-027 **Emblems in Favourites.** Each favourite card shows the team's emblem (same source, rules, caching and empty-box fallback as FR-017) at the left edge, about 48 dp square, vertically centred against the whole card (name, last result and next fixture), followed by the card's text. Favourites keep only the team id and name, so the address is always `https://crests.football-data.org/<teamId>.png`. With the dividers it gives each card a clear visual anchor.
- FR-028 **Outcome badge.** The last result starts with a round badge, 28 dp, with a letter, following the usual form-badge convention of sports apps but without relying on colour (constitution §1): **W** = solid badge (text colour fill, background-colour letter), **D** = outlined badge, **L** = faint badge (muted fill and letter). The letter is always shown. TalkBack reads "Win", "Draw" or "Loss" instead of the letter. The same badge size is used if a form strip is added later. A coloured variant (green/grey/red) is a possible later opt-in setting, not part of v1.
- FR-029 **Position in the label.** A favourite's league label (002 FR-121) also shows the team's current position in that league's table, as an English ordinal: "Serie A · 3rd", "Serie B · 1st", "· 11th", "· 22nd". The position comes from the league table (same source, saved copy and refresh rules as FR-031..034). Until the table has loaded, or if it has none, or the team is not in it any more, the label shows the league alone. Opening Favourites therefore also loads one table per league that has a favourite (not one per team).: "No favourites yet. Open a league and tap ☆ next to a team."

**Data & caching**
- FR-030 Only two request kinds: standings and matches of one competition (`X-Auth-Token` header). Matches are fetched **once per league** that contains a favourite, not per team. Nothing is fetched for screens not shown (Favourites also loads the table of each league that has a favourite, FR-029).
- FR-031 **Show saved first.** Every response is saved on disk; opening a screen shows the saved copy at once and, if it is older than 5 minutes, refreshes quietly in the background; the list is replaced in place.
- FR-039 **Reuse what is already loaded.** Data held in memory from an earlier screen is reused: a league table loaded for the Favourites view (FR-029) is shown at once on that league's tab, and the other way round, as are fixtures. A held copy younger than 5 minutes and not a failed-refresh copy is not read from disk or fetched again; Refresh (FR-033) always reloads.
- FR-032 **Offline.** If the refresh fails with a network/server/rate-limit error, the saved copy stays and a plain line "Offline – showing saved data from 14:05" (or "Rate limit reached – showing saved data from …") appears under the title. With nothing saved: the message and a Retry button.
- FR-033 A plain **Refresh** button next to Settings forces a reload of the shown screen (no pull-to-refresh, constitution §2b).
- FR-034 Rate limits (free plan: 10 requests/minute): requests are made one after another, never in loops; an HTTP 429 is shown per FR-032 and not retried automatically.
- FR-035 Dates and times use the phone's time zone and locale (`utcDate` converted).
- FR-037 **Emblems are kept.** An emblem is downloaded once, the first time it is needed, and then kept for good (they change about once a season). "Clear saved data" and "Remove API key" do **not** delete emblems, and nothing refetches an emblem that is already saved. Only uninstalling the app (or clearing its storage) removes them.
- FR-038 **Update emblems manually.** Settings → Storage has a plain "Update team emblems" button. It re-downloads every emblem that is saved (from the same address rules as FR-017/FR-120; four at a time), replacing a file only when the new download is a valid PNG, so a failed update keeps the old emblem. While it runs the button is disabled and a line says "Updating…"; afterwards it says "Updated N emblems." or "Updated N, could not update M." (or "No emblems saved yet."). Emblems on screen refresh when it finishes. Teams whose emblem was never saved are not fetched by this button; they download the first time they appear.
- FR-036 Saved responses contain only what the API returned (no key); they live in app-private storage, are excluded from backup, capped at 5 MB in total, and unreadable files are ignored and replaced. "Clear saved data" in Settings deletes them.

**Appearance & settings**
- FR-060 Style: monochrome, same colour tokens as `athena-launcher`/`podcasts`/`music` `Theme.kt`; no dynamic colour.
- FR-061 Theme (system/light/dark), typeface (same twelve), text size 80–140 %.
- FR-062 **Follow launcher** (default on): read the launcher's appearance bridge (`content://com.anderslarm.athenalauncher.appearance/appearance`, signature permission); fall back to local values field by field; last values cached so a cold start looks right.
- FR-064 Settings screen sections: Data source (API key saved, Remove API key), Storage (Clear saved data), Appearance (follow launcher, theme, typeface, text size), About (Open-source licences).
- FR-066 Licences screen listing the app, bundled fonts and libraries; also names football-data.org as the data source.

## 5. Non-functional requirements
- NFR-001 Android 16 (API 36) and 17 (API 37) only.
- NFR-002 Network: HTTPS to `api.football-data.org` and `crests.football-data.org` only; cleartext disabled.
- NFR-003 Secrets never appear in logs, errors or UI (constitution §4). Permissions: `INTERNET` and the launcher's appearance permission only.
- NFR-004 Saved standings are on screen within 0.5 s of start, before any network answer; parsing off the main thread.
- NFR-005 Accessibility: content descriptions on all controls, touch targets ≥ 48 dp, usable at 140 % text and 200 % system font scale (no clipped numbers).
- NFR-006 Robustness: null scores, missing groups, unknown statuses, empty tables, a season with no matches yet must not crash; tests cover them.

## 6. Data
- Settings (DataStore): themeMode, typeface, textScalePercent, followLauncher, apiKeyRef, favourites (JSON list of `{teamId, name, competition}`), launcher cache.
- Secret store: API key.
- Saved emblems: `filesDir/crests/<Source>-<teamId>.png`, excluded from backup, kept until uninstall (FR-037).
- Saved responses: `filesDir/football-cache/` (JSON per request + fetch time), ≤ 5 MB, excluded from backup.

## 7. Open questions
- **Serie B and Serie C (deferred).** Findings, 2026-11-10: football-data.org lists Serie B (`SB`, tier two) and the three Serie C groups (`ISCA/ISCB/ISCC`, tier four); whether the user's plan includes them is untested. API-Football has all of them for 2026 (league ids 136, 138, 942, 943) but its free plan refuses the current season ("Free plans do not have access to this season, try from 2022 to 2024"); TheSportsDB's free key returns only 5 table rows and one fixture; Sportmonks' free plan covers two non-Italian leagues. Next spec: either test the football-data key against `SB`/`ISC*`, or add a paid source behind the same repository interface.
- Cups for favourites (Coppa Italia): natural next spec, would use `/v4/teams/{id}/matches`.
- A favourite whose team is relegated keeps its league code until the user re-adds it; auto-migration is a later spec.
