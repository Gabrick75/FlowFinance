# FlowFinance — Building & Releasing

## 1. Project layout (Gradle)

```
FlowFinance/
├── build.gradle.kts          # root: AGP, Kotlin, compose, KSP, Hilt plugins (apply false)
├── settings.gradle.kts       # repositories (google/mavenCentral), includes :app
├── gradle.properties         # jvmargs -Xmx2048m, useAndroidX, nonTransitiveRClass
├── gradle/libs.versions.toml # version catalog (all versions in one place)
├── gradlew / gradlew.bat     # wrapper (Gradle 9.5.1)
├── app/
│   ├── build.gradle.kts      # module config + dependencies
│   ├── proguard-rules.pro
│   └── src/
└── docs/
```

## 2. Environment requirements

- **JDK 17** (required by current AGP) — verify with `java -version`.
- **Android Studio** (Giraffe 2023.3.1 or newer per README; latest stable recommended) or CLI + Android SDK `platforms;android-35`, build-tools, JDK 17.
- SDK Manager components: `platforms;android-35`, `build-tools` matching AGP 8.13, `platform-tools`.
- No local.properties committed; Android Studio creates it (or set `sdk.dir` manually).

## 3. Version catalog highlights (`gradle/libs.versions.toml`)

| Component | Version |
|-----------|---------|
| AGP (`com.android.application`) | 8.13.2 |
| Kotlin (`org.jetbrains.kotlin.android`) | 2.0.21 |
| Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) | 2.0.21 |
| KSP (`com.google.devtools.ksp`) | 2.0.21-1.0.27 |
| Hilt (`com.google.dagger.hilt.android`) | 2.51.1 |
| compose-bom | 2024.09.00 |

Version bumps must be co-ordinated with KSP↔Kotlin compatibility (KSK versions are Kotlin-version locked).

## 4. Module configuration (`app/build.gradle.kts`)

- `namespace = applicationId = "com.flowfinance.app"`.
- `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`, `versionCode = 5`, `versionName = "2.0.1"`.
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.
- Build types:
  - **debug** — default.
  - **release** — `isMinifyEnabled = true`, `isShrinkResources = true`, proguard default rules + `proguard-rules.pro`.
- Java/Kotlin toolchain: Java 11 source/target, `jvmTarget = 11`, **core library desugaring** enabled (`desugar_jdk_libs 2.0.4`) — required for `java.time` on API < 26.
- `buildFeatures { compose = true; buildConfig = true }`.
- `vectorDrawables.useSupportLibrary = true`.

## 5. Signing

- The committed config has **no signingConfig** for release → release builds are signed with the debug key unless you configure `signingConfigs` locally (do not commit secrets). The published APK in `app/release/` was signed during authoring.

## 6. Build commands

```bash
# Debug APK
./gradlew assembleDebug
# Release APK (unsigned-by-default / your configured keystore)
./gradlew assembleRelease
# Install on device/emulator
./gradlew installDebug
# Tests
./gradlew testDebugUnitTest          # unit tests (JUnit)
./gradlew connectedDebugAndroidTest  # instrumented tests (needs device/emulator)
# Clean
./gradlew clean
```

Outputs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 7. Tests

Current state (placeholders only):
- `app/src/test/java/com/flowfinance/app/ExampleUnitTest.kt` — sample unit test.
- `app/src/androidTest/java/com/flowfinance/app/ExampleInstrumentedTest.kt` — sample instrumented test.

There is **no existing test suite** covering the repositories, ViewModels, or workflow — a good target for future work (e.g. Room in-memory tests for DAOs, `MainDispatcherRule` for ViewModels, Compose UI tests for `AddTransactionSheet`).

## 8. ProGuard / R8 (`app/proguard-rules.pro`)

- Keep Room entity fields and Gson models (see 07-SECURITY).
- Gson keeps: `Signature, *Annotation*`, `TypeToken` and its subtypes.
- Release uses `getDefaultProguardFile("proguard-android-optimize.txt")` + project rules.

## 9. Release workflow (as observed in repo)

1. `./gradlew assembleRelease` (R8 minify+shrink).
2. APK copied to `app/release/FlowFinance-v2.0.1.apk` alongside baseline profiles (`app/release/baselineProfiles/`) and `output-metadata.json`.
3. Publish to GitHub Releases on `Gabrick75/FlowFinance/releases`.

## 10. No CI/CD

There is no GitHub Actions workflow, no scripted pipeline, no Docker image. Releasing is a local (or manual) process. If CI is desired, a minimal GitHub Actions job would be: `actions/setup-java@v4` (JDK 17) → `gradlew assembleRelease` → upload APK artifact.

## 11. Version bump checklist (vNext)

- `app/build.gradle.kts`: `versionCode`/`versionName`.
- `values/strings.xml`: `app_version_name`.
- `BackupMetadata.dbVersion` (if schema changes) — keep in sync with `AppDatabase.version`.
- When changing schema: add an explicit `Migration` in `DatabaseModule` and register it in `provideAppDatabase()` (no destructive fallback).