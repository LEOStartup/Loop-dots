# Loop Dots

Android habit tracker with month, week and history views, local statistics and five resizable home-screen widgets.

## Install v1.0.9

**[Download LoopDots-v1.0.9.apk](https://github.com/LEOStartup/Loop-dots/releases/download/v1.0.9/LoopDots-v1.0.9.apk)**

Install over the previous signed release. Do not uninstall if you want to keep your existing data. The application ID (`com.leostartup.loopdots.v2`) and persistent signing key are unchanged. Version code: 25.

## v1.0.9 popup fixes

Popup backdrops remain transparent after dismissal until their surface is removed. Backdrop mode is chosen before the opening frame, duplicate menu opens are ignored, and settings subpages fade without sideways movement. Pressing menu/settings rows no longer fades or scales their text. The approved 500 ms tab-content fade is unchanged.

## v1.0.8 transition matching

The new reference recording shows an approximately 500 ms accelerating fade-in on tab content. Only cards/statistics/settings content animates from transparent to opaque; the page title and bottom navigation stay fully painted and stationary. No outgoing page snapshot or horizontal translation is used for tabs. Value changes and repeated taps on the active tab do not restart the fade.

## v1.0.7 interaction update

- The bottom navigation remains in a permanent viewport shell while only the main content scrolls. Opening/closing sheets never changes the document position.
- Tab changes dissolve only the opaque outgoing page over an already-painted incoming page, avoiding the dark midpoint of overlapping fades. Navigation buttons no longer scale on touch.
- Regression checks sample navigation geometry throughout pressed states, scrolling, tab changes and modal transitions.

## v1.0.6 interaction update

- Live controls are updated in place: marking days, choosing colors/categories and toggling settings no longer replays a sheet opening animation.
- Animated opening and dismissal, subtle tab transitions, fixed six-week calendar with horizontal month transitions, and full-height settings subpages.
- Editing retains focus, caret, expanded options and scroll position; returning from the emoji/category picker restores the form.
- Repeated navigation cancels obsolete animations. Reduced motion is respected.
- Native widget rendering runs off the UI thread; saved data remains synchronous.
- Browser regression checks cover stable calendar frames, retained controls, close/reopen races and rapid taps. Physical Samsung/DeX validation remains device-side.

## Features

- Month cards, weekly checklist and activity history.
- Habit names, descriptions, colors, emoji, categories, daily quantities and custom-value entry.
- Build/quit habits, planned weekdays and streak goals.
- Long-press actions: complete/uncheck, calendar, notes, edit, share and archive.
- Per-habit completion statistics, monthly chart and daily/weekly/monthly streak records.
- Scheduled local reminders (delivery may be delayed by Android battery restrictions).
- Dark, light and system appearance; Portuguese and English main interface; ordering and start-page settings.
- Export/import JSON backup through Android's document picker. Imports are validated, previewed and merged by habit ID.
- Five widgets: small 2×1, compact list 4×1, medium 4×1, grid 3×2 and wide grid 4×2.
- Transparent, translucent, dark or light widget backgrounds; adjustable mark and text sizes.
- Tap small/list widget days to record; larger history widgets provide a today button and open the calendar from their history grid.

The app works offline. External project/support links open GitHub in the browser. No analytics, accounts, ads or paid subscription. Emoji appearance follows the Android system font.

## Data migration

The first launch reads the previous `habits_v2` list and `done_v2_<id>` date sets, keeping IDs, colors, icons, names and marked dates. The original keys remain intact. A `migration_snapshot_v3` is retained alongside the new `document_v3`. Each save retains the previous document. Dates are local calendar keys, never UTC-truncated timestamps.

## Build and verify

Java 17, Gradle 8.7, Android SDK 35.

```sh
npm ci
npx playwright install chromium
npm test
gradle test lint assembleRelease
```

The release workflow verifies the permanent keystore, runs tests/lint, compiles, verifies the APK signature and publishes a direct APK asset. Signing secrets are documented in [SIGNING.md](SIGNING.md).
