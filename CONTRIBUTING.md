# Contributing to Topsort Analytics SDK

Thank you for your interest in contributing! This guide covers setup, local development commands, the commit format, and the release process.

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/Topsort/topsort.kt.git
   cd topsort.kt
   ```

2. Open in Android Studio (Electric Eel or later) or IntelliJ IDEA.

3. Copy `local.properties.example` to `local.properties` and fill in your Android SDK path.

4. (Optional) Set `topsort.sample.bearertoken` in `local.properties` to run the sample app with a live token.

## Local commands

| Command | What it does |
|---|---|
| `./gradlew detekt` | Run Detekt static analysis (auto-corrects style issues) |
| `./gradlew :TopsortAnalytics:test` | Run JVM unit tests |
| `./gradlew :TopsortAnalytics:connectedCheck` | Run instrumented tests (requires a connected device or emulator) |
| `./gradlew :TopsortAnalytics:apiCheck` | Verify public API has not changed unexpectedly |
| `./gradlew :TopsortAnalytics:apiDump` | Regenerate the API dump after an intentional public API change |
| `./gradlew :TopsortAnalytics:koverHtmlReport` | Generate an HTML code coverage report |
| `./gradlew :TopsortAnalytics:dokkaHtml` | Generate API documentation |

## Public API changes

The project uses [Binary Compatibility Validator (BCV)](https://github.com/Kotlin/binary-compatibility-validator) to track the public API surface.

If your PR **intentionally** changes the public API (adds, removes, or renames a public class/method/property):

1. Make the code change.
2. Run `./gradlew :TopsortAnalytics:apiDump` to regenerate `TopsortAnalytics/api/TopsortAnalytics.api`.
3. Commit the updated `.api` file together with your code change.

If you do **not** intend to change the public API, ensure `./gradlew :TopsortAnalytics:apiCheck` passes before opening a PR.

## Commit format

This project uses [Conventional Commits](https://www.conventionalcommits.org/). Please format commit messages as:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Common types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`.

Breaking changes should include `!` after the type (e.g. `feat!:`) and/or a `BREAKING CHANGE:` footer.

Commit messages drive automatic versioning and changelog generation via [release-please](https://github.com/googleapis/release-please).

## Release process

Releases are fully automated:

1. Merge PRs to `main` using Conventional Commit messages.
2. `release-please` opens a release PR that bumps `VERSION_NAME` in `gradle.properties` and updates `CHANGELOG.md`.
3. When the release PR is merged, a GitHub Release is created automatically.
4. The `Release` workflow publishes the library to Maven Central.

Maintainers do not need to manually tag releases or edit changelogs.

## Code style

Detekt enforces code style. Run `./gradlew detekt` before pushing — it auto-corrects most issues. The configuration is in `detekt.yaml`.
