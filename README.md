# Walley

A personal finance tracker for expenses, income, savings, bank accounts, and investments — built as a native Android app.

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (dynamic color on Android 12+)
- **Architecture:** MVVM, single `:app` module for now (data/domain/feature modules can be split out later if the codebase grows)
- **Persistence:** Room (local SQLite) — all data stays on-device
- **DI:** Hilt
- **Async:** Kotlin Coroutines & Flow
- **Navigation:** Navigation Compose
- **Build:** Gradle (Kotlin DSL) with a version catalog (`gradle/libs.versions.toml`)
- **Min/target SDK:** 26 / 34 (covers the Galaxy S24 and effectively all active Android devices)

## Getting started

You can use either Android Studio or IntelliJ IDEA Ultimate — this is a plain Gradle project, so both open it the same way:

1. Open the project root.
   - **Android Studio** (Koala or newer): Android support is built in.
   - **IntelliJ IDEA Ultimate**: enable the bundled **Android** plugin (Settings → Plugins → Android) first. Note this requires the *Ultimate* edition — Community does not support Android development.
2. Point the IDE at an installed Android SDK (SDK Manager) if it doesn't detect one automatically — needed to compile against `compileSdk 34`.
3. Let Gradle sync — the wrapper (`./gradlew`) will download Gradle 8.9 and all dependencies automatically.
4. Run the `app` configuration on an emulator or your Galaxy S24 (enable Developer Options + USB debugging to run over USB).

Since the UI is all Jetpack Compose (no XML layouts), you won't miss Android Studio's visual Layout Editor. Compose Preview and standard debugging work the same in IntelliJ IDEA Ultimate; a few extras (Layout Inspector, some profilers/App Inspection tooling) are Android Studio-only.

From the command line:

```bash
./gradlew assembleDebug   # build a debug APK
./gradlew test            # run unit tests
./gradlew connectedAndroidTest  # run instrumented tests on a connected device/emulator
```

## Project layout

```
app/src/main/kotlin/com/walley/app/
├── MainActivity.kt        # single-activity host, sets up Compose content
├── WalleyApplication.kt   # Hilt entry point
└── ui/theme/              # Material 3 theme, color, and typography
```

As features are added, the plan is to grow this into `data/` (Room entities, DAOs, repositories), `domain/` (use cases), and `feature/<name>/` (screens + view models) packages.
