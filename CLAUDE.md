# Athena Calcio

A minimalist, monochrome Android app (Kotlin + Jetpack Compose) showing the standings of Serie A (football-data.org) and Serie B (ESPN, unofficial, spec 002), with favourite teams and each favourite's last result and next fixture. Sibling of Athena Launcher (`../athena-launcher`), Athena Podcasts (`../athena-podcasts`) and Athena Music (`../athena-music`). Data: football-data.org with the user's own API key (same source as `../mappa-del-calcio`).

## Spec-driven workflow (GitHub Spec Kit)
Spec Kit is installed (`.specify/`, skills in `.claude/skills`: `/speckit-specify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, `/speckit-clarify`, `/speckit-analyze`). Read in this order before changing anything:
1. `.specify/memory/constitution.md` – principles that always apply
2. `specs/001-calcio-core/spec.md` – what and why
3. `specs/001-calcio-core/plan.md` – how
4. `specs/001-calcio-core/tasks.md` – ordered work; tick items when done

Rules: no behaviour that is not in `spec.md` (update the spec first); requirement IDs (FR-/NFR-) stay stable; mark unknowns `[NEEDS CLARIFICATION]`; new features get their own `specs/00N-…` folder.

## Targets
- minSdk 36, target/compile 37. Kotlin, Compose, single activity, DataStore, OkHttp, kotlinx-serialization. Package / application id `com.anderslarm.athenacalcio`. MIT.
- Only network peer: `https://api.football-data.org/v4` with the `X-Auth-Token` header. The API key is entered in the app, stored encrypted (Keystore) and **never committed, logged or shown**. Fetch only what is on screen; no background work.
- Competitions: `SA` Serie A (football-data.org, key) and `SB` Serie B (ESPN `ita.2`, unofficial, no key, per-team schedule calls: see `specs/002-serie-b-espn`). Serie C (`ISCA/ISCB/ISCC`) is still deferred: nothing free found (spec 001 §7). A football-data.org 403 is shown as "plan does not include this league", not as a bad key.
- Look and feel mirrors the other Athena apps (`ui/theme/Theme.kt` copied; do not diverge without updating the spec). Appearance follows the launcher through its content-provider bridge.
- v1 is deliberately small: standings, favourites, last/next league fixture, saved copy for offline. No cups, crests, notifications, widgets, top scorers.

## Build
```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest lintDebug
```
Install debug builds signed with the shared `~/.android/debug.keystore` so the launcher appearance bridge works.
