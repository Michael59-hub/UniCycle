# UniCycle (Kotlin Multiplatform + Compose Multiplatform)

Built from the UniCycle Complete Build Specification. Shared UI lives in `composeApp/src/commonMain`.

## Run (Android)
1. Open this folder in Android Studio (Koala or newer), JDK 17+.
2. Let Gradle sync. The project ships with the Gradle **8.9** wrapper (`gradle/wrapper`), so do not change
   "Use Gradle from" away from "gradle-wrapper.properties".
3. Run the `composeApp` configuration.

## Run (iOS, Mac only)
Generate an Xcode project at https://kmp.jetbrains.com, then replace its Swift files with `iosApp/iosApp/*.swift`.
The framework name must stay `ComposeApp`. The iOS platform code (`Platform.ios.kt`) is untested.

## What is real vs prototype
- Real and persisted on-device: onboarding, profile, quiz engine (practice / mock exam / revision), autosave and resume,
  results with topic breakdown, XP ledger, streaks, notes, flashcards (spaced repetition), study planner,
  saved questions, search, reports, local community posts, themes, accessibility settings, text-to-speech.
- Prototype data (see `data/PrototypeContent.kt`): institutions, courses and 15 sample questions, all labelled "Under review".
  The UI only reads `ContentRepository`, so swap in an API-backed implementation without touching screens.
- Not connected: accounts (no password is stored or verified), email/phone verification, password reset, AI
  (`UnconfiguredAiService` returns an honest failure), leaderboards, tournaments, messaging, push notifications.

## Fonts
The spec asks for Sora (display) and Inter (body). Font files are not bundled, so `Theme.kt` falls back to the system sans-serif.
To enable them: add `compose.components.resources`, put the .ttf files in `composeResources/font/`, and set
`DisplayFont` / `BodyFont` in `Theme.kt`.

## Gradle version
Pinned to 8.9 with AGP 8.5.2 and Kotlin 2.0.21. Gradle 9 breaks this AGP version
(`DefaultArtifactPublicationSet` error), so keep the wrapper as shipped.
