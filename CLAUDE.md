# CLAUDE.md — analytics.kotlin

Open-source Android SDK (`com.topsort:topsort-kt`) for the Topsort retail media platform.
Modules: `:TopsortAnalytics` (library), `:app` (sample).
See [CONTRIBUTING.md](CONTRIBUTING.md) for full setup, release process, and contribution guide.

## Development Environment

- **JDK 17 required** (Temurin). Later JDKs are **incompatible** — Groovy DSL fails with "Unsupported class file major version".
- Always prefix Gradle commands with:
  ```
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
  ```
- Always use `./gradlew` (wrapper 9.4.1), never system Gradle.

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
Cache (SharedPreferences)     ◄── persistent; event body and record counter
    │                             written in one editor
    ▼
WorkManager                   ◄── one unique work unit per cached record,
    │                             network-constrained; a failure is isolated
    ▼                             to its own event
TopsortAnalyticsHttpService   ◄── POST /v2/events
```

A record is deleted only once a worker has actually run for it, so anything still in the cache is
by definition undelivered. `Analytics.setup()` therefore enqueues `PendingEventSweepWorker`, which
re-enqueues undelivered records — at most `MAX_RESEND_PER_SWEEP` per run, so a large backlog drains
across several launches. The sweep runs on a WorkManager thread, never inline in `setup()`: reading
the cache decrypts every record and pruning writes synchronously.

Nothing is discarded for being old. Whether a late event still attributes depends on the
marketplace's attribution window, and whether it is still billable depends on the campaign's charge
type — a CPM impression is chargeable long after it can attribute — and both facts live server-side.
The client's only bound is `MAX_CACHED_RECORDS`: when the cache exceeds it the oldest records are
evicted, enforced off the enumeration the sweep already performs so it costs nothing extra. That is
a resource decision, which this side can make correctly, rather than a billing one, which it cannot.

The sweep also deletes a record whose event type cannot be determined — nothing knows which
endpoint it belongs to, so it can never be sent by anyone.

Events are never enqueued onto a shared work chain. A chain couples unrelated events — work
appended after a terminal failure never runs, which used to silence an install permanently after a
single 4xx.

Package layout:
- `com.topsort.analytics.Analytics` — main singleton, implements `TopsortAnalytics` interface
- `com.topsort.analytics.UserIdentity` — sealed identity passed to `setup` (`UserIdentity.of(id)` → `Identified` / `Unidentified`)
- `com.topsort.analytics.EventDiscardListener` / `DiscardReason` — optional host callback for every undelivered event the SDK drops; `Cache.discard` is the single exit
- `com.topsort.analytics.model/` — event data models (Impression, Click, Purchase, Placement, Entity)
- `com.topsort.analytics.model.auctions/` — auction models (Auction, AuctionRequest/Response, AuctionError, ApiConstants)
- `com.topsort.analytics.banners/` — BannerView, BannerConfig (sealed), banner auction helpers
- `com.topsort.analytics.service/` — HTTP services (AuctionsHttpService interface, implementations)
- `com.topsort.analytics.core/` — HttpClient, JsonExtensions, RandomGenerator, EventTimestamp
- `com.topsort.analytics.worker/` — EventEmitterWorker (one work unit per cached event), PendingEventSweepWorker (recovery of undelivered events)

## SDK Design Principles

- **`internal` by default** — all new classes/functions must be `internal` unless explicitly part of the public API.
- **Binary compatibility enforced** — BCV tracks the public API in `TopsortAnalytics/api/TopsortAnalytics.api`. Run `apiCheck` before every PR. Run `apiDump` only after a deliberate public API decision.
- **Minimal dependency footprint** — do not add dependencies without strong justification. Each dep is transitive to every consumer.
- **Manual JSON serialization** — use `org.json` + `JsonSerializable` interface. No reflection-based libraries (Gson, Moshi, kotlinx.serialization) to keep APK size small and avoid proguard complexity.
- **Factory companion objects** — deserialization via `fromJsonObject()` / `fromJsonArray()` on companion.
- **Sealed classes for closed hierarchies** — errors (`AuctionError`), configs (`BannerConfig`), enums where exhaustive matching matters.
- **Graceful degradation** — if `Analytics.setup()` not called, events are logged but not sent. Never crash the host app.
- **Thread safety** — coroutines + `SupervisorJob` for background work, `AtomicBoolean` for flags. `SharedPreferences.apply()` for writes that can happen on the caller's thread; `commit()` only from worker threads, where losing the write would cost an event or duplicate one.

## SDK Anti-patterns

- Do NOT add reflection-based serialization libraries (Gson, Moshi, kotlinx.serialization).
- Do NOT leak `Context` references — use `applicationContext` only, never Activity/Fragment context.
- Do NOT add heavyweight dependencies (OkHttp, Retrofit) — the SDK uses `HttpURLConnection` intentionally.
- Do NOT throw unchecked exceptions from public API — use sealed error types or nullable returns.
- Do NOT block the main thread — all network I/O goes through WorkManager/coroutines.
- Do NOT change the public API surface without updating `TopsortAnalytics/api/TopsortAnalytics.api` via `apiDump`.
- Do NOT hardcode API endpoints — use `ApiConstants`.

## Code Conventions

- Kotlin, Java 11 source/target, JVM toolchain 17 (build requires JDK 17, bytecode targets JVM 11).
- Detekt enforces style (config: `detekt.yaml`). Run before pushing.
- RFC3339 timestamps via `SimpleDateFormat` (`eventNow()` helper in `EventTimestamp.kt`); no date library.
- `JsonSerializable` interface for all models that go over the wire.
- Null-safe JSON via extensions in `JsonExtensions.kt` (`getStringOrNull`, `getIntOrNull`, `getStringListOrNull`).
- Test naming: backtick descriptive names (`` `json click serialization`() ``) or snake_case.
- Test data builders: `TestObjects.kt` (unit) / `TestObjectsAndroid.kt` (instrumented).
- Test frameworks: JUnit 4 + AssertJ assertions + MockK mocking + kotlinx-coroutines-test.
- Service mocking: `TopsortAuctionsHttpService.setMockService()` / `.resetToDefaultService()`.
- Coverage: `jacocoMergedReport` merges unit + instrumented execution data (the gate, 78% lines, runs in the instrumented CI job); Kover reports the JVM-only slice with a 35% floor in the unit-test job. All new public API must have unit tests.

## Git Workflow

- **Never commit directly to `main`.** All changes go through PRs from a dedicated branch.
- Branch names should be descriptive (e.g., `feat/add-google-environment`, `fix/merge-pagination-offset`).
- **Large changes must be broken into stacked PRs** — each PR should be independently reviewable and represent a single logical unit of work. Avoid monolithic PRs that touch many unrelated things at once.
- Each PR in a stack should be based on the previous branch, not `main`, so they can be reviewed and merged in order.
- **After a stacked PR is squash-merged**, all child branches must be rebased onto the updated `main` to drop the now-duplicate commits. Squash merges create a new SHA on `main`, so the original commits appear as unrelated changes in child PRs if not rebased. Use `git rebase --onto main <old-base> <child-branch>` to cleanly re-root.
- **Before opening any PR**, verify that `git log --oneline main..HEAD` only shows commits belonging to your change. If you see commits from already-merged PRs, rebase first.
- **Admin override** (`gh pr merge --admin`) is only appropriate to bypass the review requirement when all CI checks pass. Never use it to force-merge a PR with failing CI — fix the failures first.
- Keep branches up to date with `main` before merging — rebase or merge `main` into your branch to resolve conflicts locally, not in the merge commit.
- Use [Conventional Commits](https://www.conventionalcommits.org/) for all commit messages (e.g., `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).
- Never approve or merge a PR that has unresolved review comments — address or explicitly dismiss each one first. Always check nested/threaded comments (e.g. replies under bot comments) as they may contain substantive issues not visible at the top level.
- Before merging with `--admin`, wait at least **5 minutes** after the PR is opened. This gives Bugbot and other async bots time to post their comments. After the wait, check all PR comments (including nested/threaded replies) for unresolved issues before merging. Run the wait in the background and do **not** block on `TaskOutput` — let the completion notification come to you so the session stays responsive.
- **Project-specific**: run `apiCheck` before pushing any PR that touches library source.
- **After every rebase in a stack, assert before pushing.** A rebase that silently replays nothing
  looks identical to one that succeeded, and the push that follows destroys work. Both of these
  have happened here. Check two numbers against what you expect, and treat a mismatch as a stop:
  ```bash
  git rev-list --count <base-branch>..<branch>   # commits the PR should contribute
  ./gradlew :TopsortAnalytics:connectedDebugAndroidTest   # test count should not drop
  ```
  - **Commit count.** `git rebase --onto <newbase> <upstream> <branch>` replays `<upstream>..<branch>`.
    Passing the branch's *own* tip as `<upstream>` makes that range empty, so the branch is silently
    reset onto the new base and every commit is dropped. `<upstream>` is the branch's **old base**,
    never its own tip. `Rebasing (1/N)` in the output is the cheapest confirmation that N commits
    were actually replayed.
  - **Test count.** A dropped test file does not fail the build — it just stops being run. If the
    instrumented count falls after a rebase, something was lost. Know the expected number before
    you start.
- **Never push from a dirty working tree.** `git push` sends committed state; a green test run
  against uncommitted fixes proves nothing about what lands. Verify
  `git status --porcelain` is clean, and that `git rev-parse <branch>` matches
  `git rev-parse origin/<branch>` after pushing.
- **Verify a scripted edit applied.** A `replace` that matches nothing is a no-op that reports
  success. Assert the pattern was found, or grep the result — do not trust the script's own output.

## CI Pipeline

- **PRs**: `detekt` + `apiCheck` (lint.yaml), unit tests + kover, then instrumented tests + merged coverage gate (tests.yaml)
- **Push to main**: Dokka → GitHub Pages (docs.yaml), release-please PR (release-please.yaml)
- **GitHub Release**: publish to Maven Central (publish-to-maven.yaml)
