# Plan 001 – Calcio Core

Implements `spec.md`. Constrained by `.specify/memory/constitution.md`.

## 1. Stack
| Concern | Choice | Why |
|---|---|---|
| Language/UI | Kotlin, Jetpack Compose (Material 3), single activity | Constitution §7; theme code copied from sibling apps |
| Build | Gradle KTS, version catalog, AGP 9 (built-in Kotlin) | Versions copied from `athena-music/gradle/libs.versions.toml` (Media3 entries dropped) |
| SDK | minSdk 36, target/compile 37 | Constitution §6 |
| HTTP/JSON | OkHttp + kotlinx-serialization-json | Small, already used in sibling apps; one client for one host |
| Settings | DataStore Preferences | As siblings; favourites stored as a JSON string |
| Secrets | Android Keystore AES-GCM wrapper (copied from Music `SecretStore`) | No deprecated security-crypto |
| State | ViewModel + StateFlow, manual `AppContainer` | As siblings |
| Tests | JUnit 4 + MockWebServer | |
No new dependency beyond the siblings' set.

## 2. Layout (`se.anders.athenacalcio`)
```
MainActivity, AthenaCalcioApplication
core/       AppContainer
data/
  football/ FootballClient (OkHttp), models + DTO mapping, FootballRepository (cache + freshness), Redact
  settings/ SettingsRepository (+ Favourite list)
  secrets/  SecretStore
  launcher/ LauncherAppearanceSource
  licences/ Licences
domain/     Competition, TeamFixtures (last/next), EffectiveAppearance
ui/         MainViewModel, FavouritesViewModel/StandingsViewModel (screen state), Formatting
  key/ standings/ favourites/ settings/ licences/ components/ theme/
```

## 3. Key decisions
### 3.1 Data source
`https://api.football-data.org/v4`, header `X-Auth-Token`. The user's key is entered in the app (never in the repo). Same provider as `../mappa-del-calcio` (`backend/app.js` uses `/v4/competitions/SA/matches` with `FOOTBALL_DATA_API_KEY`).
Probed without a key on 2026-11-10 (`GET /v4/competitions`): `SA` Serie A is TIER_ONE (free). Serie B (`SB`) and Serie C (`ISCA/ISCB/ISCC`) exist but are deferred (spec §7).

### 3.2 Endpoints
`GET /competitions/{code}/standings`, `GET /competitions/{code}/matches`, and `GET /competitions/SA` as the key check. Nothing else. One `matches` call returns the whole season (≈380 matches for Serie A, a few hundred KB): last/next for *all* favourites in that league come from it, which keeps us far below the 10 requests/minute free limit (only one league today, so a cold start is one call; extensible to more leagues).

### 3.3 Error mapping
`FootballException(kind)`: `Network` (IOException), `Auth` (401, or 400/403 whose `message` mentions "token"), `PlanRestricted` (403 otherwise), `RateLimited` (429), `Server` (5xx / other), `Parse`. UI maps kinds to strings; `Auth` anywhere sends the user back to the key screen (FR-003).

### 3.4 Cache (FR-031..036)
`ResponseCache(dir, clock)` writes `{fetchedAt, body}` JSON per key (`standings-SA`, `matches-SA`, …), capped at 5 MB (oldest deleted first). `FootballRepository.load(kind, competition, force)` returns a `Flow<Loaded<T>>`: first the saved copy (`stale` flag if > 5 min), then the fresh one, or `Loaded.Failed` carrying the saved copy if the refresh fails. Parsing is on `Dispatchers.Default`.

### 3.5 Last / next fixture (FR-023)
Pure function `fixturesFor(teamId, matches): TeamFixtures(last, next)` in `domain/`. Last = max `utcDate` among `FINISHED`/`AWARDED` with a non-null full-time score; next = min `utcDate` among the upcoming statuses; live matches count as next. Unknown statuses are ignored.

### 3.6 Favourites
`Favourite(teamId, name, competition)` list serialised with kotlinx-serialization into one DataStore string key; toggling is idempotent on `(teamId, competition)`. Unreadable JSON → empty list.

### 3.7 Navigation
`MainViewModel` holds a screen stack (`Main`, `Settings`, `Licences`; the API key form is a state, not a screen) and the selected tab (`Favourites`, `SerieA`).

### 3.7b Landscape table (FR-016)
`StandingsScreen` reads `LocalConfiguration.current.orientation`; landscape selects a `DetailedRow`/`DetailedHeader` with ten columns, portrait keeps the compact row. Same `LazyColumn` and same data, so rotation needs no reload. The activity is recreated on rotation but the ViewModels (and so the loaded data) survive.

### 3.8 Appearance
Copied from Music: `Theme.kt`, fonts, `LauncherAppearanceSource`, `effectiveAppearance`, same Settings names. Bridge needs both apps signed with the same key (shared debug keystore on this Mac).

### 3.9 Why no Retrofit / Room / Coil
OkHttp + a few DTOs cover two endpoints; the cache is flat JSON files.

### 3.10 Emblems (FR-017)
Probed 2026-11-10: `https://crests.football-data.org/<id>.png` answered 200 `image/png` for 24 Serie A-range team ids, while `.svg` is missing for some (e.g. 100, 113, 445, 5911): so PNG is the only format used. `crestUrl(team)` (pure, tested) picks the API's `crest` if it is an https `.png` on that host, else `<id>.png`. `CrestStore` (OkHttp + `BitmapFactory`, no image library: Coil would add a dependency for one small job) downloads to `filesDir/crests`, validates `image/png` and size, decodes with `inSampleSize` to about 64 px, and keeps a small `LruCache`. A `LocalCrestStore` composition local feeds `CrestImage`, which draws a fixed 28 dp box so rows never shift.
