# Walley

A personal finance tracker for expenses, income, savings, bank accounts, budgets, investments, and assets — built as a native Android app.

See [FEATURES.md](FEATURES.md) for a full walkthrough of what's implemented.

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
├── core/
│   ├── format/            # money/currency formatting helpers
│   └── ui/                # shared Compose components (e.g. PieChartCard)
├── data/
│   ├── local/              # Room entities, DAOs, TypeConverters, WalleyDatabase + migrations
│   ├── datastore/           # DataStore-backed prefs (settings, app lock)
│   ├── remote/              # Frankfurter FX API client
│   └── repository/         # repository interfaces + impls bridging data sources and the domain layer
├── domain/model/          # plain Kotlin domain models (Account, Investment, Budget, Asset, ...)
├── di/                    # Hilt modules (database, repositories)
├── navigation/            # NavHost + bottom-tab/pager host screen
├── feature/
│   ├── home/               # net worth overview + currency breakdown pie chart
│   ├── accounts/            # bank accounts (checking/cash/investment/saving)
│   ├── budget/              # monthly budgets: creation wizard, list, detail, payment tracking
│   ├── investments/         # investment holdings linked to investment accounts
│   ├── assets/              # non-liquid assets (purchase vs. current value)
│   ├── settings/            # base currency, preferences
│   └── lock/                # PIN/biometric app lock
└── ui/theme/              # Material 3 theme, color, and typography
```
