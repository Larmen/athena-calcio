# Tasks 001 – Calcio Core

Scope narrowed 2026-11-10: Serie A only (Serie B/C deferred, spec §7).

Tick when done. Each task leaves the app building.

## Phase 0 – Setup
- [x] T0.1 Spec Kit files (`.specify/`, `.claude/skills`), constitution, spec, plan
- [x] T0.2 Gradle project, version catalog, manifest, theme/fonts/licences copied from Music (FR-060, NFR-001)
- [x] T0.3 CI workflow

## Phase 1 – Data (FR-001..003, 030..036)
- [x] T1.1 Models, `Competition`, DTO parsing + unit tests (standings, matches, null scores, unknown status)
- [x] T1.2 `FootballClient`: standings, matches, key check, error mapping + MockWebServer tests
- [x] T1.3 `ResponseCache` + `FootballRepository` freshness/offline logic + tests
- [x] T1.4 `Redact` helper + test (NFR-003)

## Phase 2 – Domain (FR-020..025)
- [x] T2.1 `fixturesFor` (last/next) + tests
- [x] T2.2 Favourites storage in `SettingsRepository` + tests

## Phase 3 – Account & settings (FR-001..004, 060..066)
- [x] T3.1 Secret store, API key state, key screen
- [x] T3.2 Launcher appearance bridge client, effective appearance + test
- [x] T3.3 Settings screen, licences screen

## Phase 4 – Screens (FR-010..015, 020..025, 031..033)
- [x] T4.1 Main screen: tab buttons (Favourites, Serie A), Refresh/Settings
- [x] T4.2 Standings screen with star buttons, offline/plan messages
- [x] T4.3 Favourites screen with last/next

- [x] T4.4 Landscape detailed table (FR-016) – built; layout not yet seen with real data (docs/manual-qa.md)

- [x] T4.5 Team emblems in table rows (FR-017, FR-037): `crestUrl`, `CrestStore`, `CrestImage` + tests
- [x] T4.6 Emblems in the Favourites view (FR-027)
- [x] T4.7 Remove league label in Favourites (FR-021); outcome badge for the last result (FR-028)
- [x] T4.8 Keep emblems for good; "Update team emblems" button in Settings (FR-037, FR-038)
- [x] T4.9 Current league position in the favourite's league label (FR-029): `Formatting.ordinal`, `positionOf`, standings for favourites' leagues
- [x] T4.10 Reuse in-memory tables and fixtures across tabs (FR-039): `Loaded.isFresh` + Browse view-model

## Phase 5 – Done
- [x] T5.1 Build, unit tests (34), lint green; README updated; key screen verified on API 37 emulator 2026-11-10 (main screens need a real key: T5.2)
- [ ] T5.2 Manual QA with the real API key (`docs/manual-qa.md`)
