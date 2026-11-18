# Calcio Constitution

Non-negotiable principles. Specs, plans and code must comply; if one cannot, amend this file first.
Shares its spirit with the Athena Launcher, Athena Podcasts and Athena Music constitutions.

## 1. Calm by default
A quiet, monochrome app: no badges, no push alerts, no live-score tickers, no ads, no recommendations, no decorative animation. Colour is never used to carry meaning (no green/red zones); text and position do.

## 2. Essentials first
Standings of Serie A (Serie B and C are deferred to a later spec), favourite teams, and each favourite's last result and next fixture. Everything else (cups, top scorers, line-ups, notifications, widgets, other countries, match details) is out of v1 unless a spec says otherwise.

## 2b. Buttons, not gestures
No swipe, pull-to-refresh, long-press or drag. Every action is a visible button on the item it applies to.

## 3. One data source, nothing else
The only network peers are `api.football-data.org` (with the user's own API key) and its image host `crests.football-data.org` (team emblems, no key sent), both over HTTPS. For Serie B (spec 002) also ESPN's public endpoints `site.api.espn.com` and `a.espncdn.com`, an **unofficial, keyless** source: best-effort, clearly labelled in the app, kept behind its own interface, and never sent the football-data.org key. No analytics, no crash-reporting SDKs, no ads, no other services. No background polling: data is fetched only while the app is open and on screen.

## 4. Secrets stay secret
The API key is stored app-private, encrypted with an Android Keystore key. Never log it, never show it (beyond a masked "key saved"), never put it in exports, URLs or crash output. It is entered at first run and never committed to the repository.

## 5. Quiet background work
None. No WorkManager jobs, no services, no wake locks, no notifications.

## 6. Latest two Android versions only
`minSdk` 36 (Android 16), `targetSdk`/`compileSdk` 37 (Android 17). No compatibility shims.

## 7. Modern, boring Android
Kotlin, Jetpack Compose, single activity, unidirectional data flow, DataStore. Every new dependency needs a one-line justification in `plan.md`.

## 8. Spec first
No feature code without a spec section and tasks. Behaviour changes update `spec.md` in the same commit.

## 9. Own the code, respect licences
Fresh MIT-licensed implementation. Every bundled font/library is listed on the Licences screen before it ships. Match data belongs to football-data.org and is used under the terms of the user's own plan; nothing is bundled or redistributed.

## 10. One look across Athena apps
Same monochrome tokens, typefaces, text scale and light/dark behaviour as Athena Launcher, Podcasts and Music; appearance follows the launcher (bridge) and falls back to local settings.

## 11. Tested where it hurts
Unit tests for domain logic (response parsing, last/next fixture selection, favourites storage, cache freshness, appearance resolution, redaction). Manual checklist for anything needing the real API key.
