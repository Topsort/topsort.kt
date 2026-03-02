# CLAUDE.md — analytics.kotlin

Open-source Android SDK (`com.topsort:topsort-kt`) for the Topsort retail media platform.
Modules: `:TopsortAnalytics` (library), `:app` (sample).
See [CONTRIBUTING.md](CONTRIBUTING.md) for full setup, release process, and contribution guide.

## Development Environment

- **JDK 17 required** (Temurin). JDK 25 is the system default but is **incompatible** — Groovy DSL fails with "Unsupported class file major version 69".
- Always prefix Gradle commands with:
  ```
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
  ```
- Always use `./gradlew` (wrapper 9.3.1), never system Gradle.

## Essential Commands

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :TopsortAnalytics:test        # Unit tests
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew detekt                          # Static analysis
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :TopsortAnalytics:apiCheck     # Verify API compat
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew :TopsortAnalytics:apiDump      # Regenerate API dump
```

See CONTRIBUTING.md for kover, dokka, and connectedCheck commands.

## Architecture

Event pipeline:
```
Analytics.report*()
    │
    ▼
Cache (SharedPreferences)     ◄── persistent, synchronous
    │
    ▼
EventPipeline (DataStore)     ◄── batching, coroutines
    │
    ▼
WorkManager                   ◄── background, network-constrained
    │
    ▼
TopsortAnalyticsHttpService   ◄── POST /v2/events
```

Package layout:
- `com.topsort.analytics.Analytics` — main singleton, implements `TopsortAnalytics` interface
- `com.topsort.analytics.model/` — event data models (Impression, Click, Purchase, Placement, Entity)
- `com.topsort.analytics.model.auctions/` — auction models (Auction, AuctionRequest/Response, AuctionError, ApiConstants)
- `com.topsort.analytics.banners/` — BannerView, BannerConfig (sealed), banner auction helpers
- `com.topsort.analytics.service/` — HTTP services (AuctionsHttpService interface, implementations)
- `com.topsort.analytics.core/` — HttpClient, JsonExtensions, RandomGenerator, EventTimestamp
- `com.topsort.analytics.worker/` — EventEmitterWorker (WorkManager background processing)

## SDK Design Principles

- **`internal` by default** — all new classes/functions must be `internal` unless explicitly part of the public API.
- **Binary compatibility enforced** — BCV tracks the public API in `TopsortAnalytics/api/TopsortAnalytics.api`. Run `apiCheck` before every PR. Run `apiDump` only after a deliberate public API decision.
- **Minimal dependency footprint** — do not add dependencies without strong justification. Each dep is transitive to every consumer.
- **Manual JSON serialization** — use `org.json` + `JsonSerializable` interface. No reflection-based libraries (Gson, Moshi, kotlinx.serialization) to keep APK size small and avoid proguard complexity.
- **Factory companion objects** — deserialization via `fromJsonObject()` / `fromJsonArray()` on companion.
- **Sealed classes for closed hierarchies** — errors (`AuctionError`), configs (`BannerConfig`), enums where exhaustive matching matters.
- **Graceful degradation** — if `Analytics.setup()` not called, events are logged but not sent. Never crash the host app.
- **Thread safety** — coroutines + `SupervisorJob` for background work, `AtomicBoolean` for flags, `SharedPreferences.apply()` for async writes.

## SDK Anti-patterns

- Do NOT add reflection-based serialization libraries (Gson, Moshi, kotlinx.serialization).
- Do NOT leak `Context` references — use `applicationContext` only, never Activity/Fragment context.
- Do NOT add heavyweight dependencies (OkHttp, Retrofit) — the SDK uses `HttpURLConnection` intentionally.
- Do NOT throw unchecked exceptions from public API — use sealed error types or nullable returns.
- Do NOT block the main thread — all network I/O goes through WorkManager/coroutines.
- Do NOT change the public API surface without updating `TopsortAnalytics/api/TopsortAnalytics.api` via `apiDump`.
- Do NOT hardcode API endpoints — use `ApiConstants`.

## Code Conventions

- Kotlin, Java 17 source/target, JVM toolchain 17.
- Detekt enforces style (config: `detekt.yaml`). Run before pushing.
- RFC3339 timestamps via Joda-Time (`eventNow()` helper in `EventTimestamp.kt`).
- `JsonSerializable` interface for all models that go over the wire.
- Null-safe JSON via extensions in `JsonExtensions.kt` (`getStringOrNull`, `getIntOrNull`, `getStringListOrNull`).
- Test naming: backtick descriptive names (`` `json click serialization`() ``) or snake_case.
- Test data builders: `TestObjects.kt` (unit) / `TestObjectsAndroid.kt` (instrumented).
- Test frameworks: JUnit 4 + AssertJ assertions + MockK mocking + kotlinx-coroutines-test.
- Service mocking: `TopsortAuctionsHttpService.setMockService()` / `.resetToDefaultService()`.
- Kover coverage threshold: 35% minimum. All new public API must have unit tests.

## Git Workflow

Follow the git workflow in `~/.claude/CLAUDE.md` (never commit to main, feature branches, conventional commits, stacked PRs for large changes).
Project-specific: run `apiCheck` before pushing any PR that touches library source.

## CI Pipeline

- **PRs**: `detekt` + `apiCheck` (lint.yaml), unit tests + kover + instrumented tests (tests.yaml)
- **Push to main**: Dokka → GitHub Pages (docs.yaml), release-please PR (release-please.yaml)
- **GitHub Release**: publish to Maven Central (publish-to-maven.yaml)
