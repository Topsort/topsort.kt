# Changelog

## [3.2.0](https://github.com/Topsort/topsort.kt/compare/v3.1.0...v3.2.0) (2026-08-27)


### Features

* make an absent user identity something a caller has to state ([#171](https://github.com/Topsort/topsort.kt/issues/171)) ([ab6b81d](https://github.com/Topsort/topsort.kt/commit/ab6b81ddef699acf9c4d4550100478ddd06aff8a))


### Bug Fixes

* make the Java sample compile again, and build it in CI ([#172](https://github.com/Topsort/topsort.kt/issues/172)) ([cfcc274](https://github.com/Topsort/topsort.kt/commit/cfcc274de096519586454cfacb8108dfd098b786))

## [3.1.0](https://github.com/Topsort/topsort.kt/compare/v3.0.0...v3.1.0) (2026-08-26)


### Features

* publish automatically when release-please cuts a release ([#169](https://github.com/Topsort/topsort.kt/issues/169)) ([38c296d](https://github.com/Topsort/topsort.kt/commit/38c296dc2a96c6e50ce38b67668a65d08a647bcd))


### Bug Fixes

* let the Maven publish be triggered manually ([#164](https://github.com/Topsort/topsort.kt/issues/164)) ([8d98362](https://github.com/Topsort/topsort.kt/commit/8d983626aef4c8285d6d174b69756f14906f8cf2))
* make the source jar wait for the generated version file ([#166](https://github.com/Topsort/topsort.kt/issues/166)) ([6c183f8](https://github.com/Topsort/topsort.kt/commit/6c183f8ec66bcb4886d891e47f6899a38d6b1c16))
* refuse to publish a ref that is not a matching release tag ([#168](https://github.com/Topsort/topsort.kt/issues/168)) ([b9d203a](https://github.com/Topsort/topsort.kt/commit/b9d203a215be3e431608cc575cb8741f261287d9))
* report a resolved bid's impression at most once per process ([#170](https://github.com/Topsort/topsort.kt/issues/170)) ([182397c](https://github.com/Topsort/topsort.kt/commit/182397cf188a93e2313522359c71a2e4a0044419))

## [3.0.0](https://github.com/Topsort/topsort.kt/compare/v2.0.1...v3.0.0) (2026-08-25)


### ⚠ BREAKING CHANGES

**No source migration is required to upgrade from 2.0.1.** This is a major release because
public signatures changed shape, not because an API you were using behaves differently.

* **A recompile is required.** The reporting methods (`reportImpressionPromoted`,
  `reportClickPromoted`, `reportImpressionOrganic`, `reportClickOrganic`, `reportPurchase`)
  and the `Click`, `Impression`, `Purchase` and `Auction` types gained new optional
  parameters. They are *appended*, so existing Kotlin call sites compile unchanged, whether
  they pass arguments positionally or by name. Binary compatibility is broken though: code
  compiled against 2.0.1 and not rebuilt will fail with `NoSuchMethodError`.
* **Java callers must pass the new arguments explicitly**, because Kotlin default arguments
  do not generate Java overloads.
* `deviceType`, `channel` and `clickType` are enums (`DeviceType`, `Channel`, `ClickType`)
  rather than `String`. These fields were added after 2.0.1 and never shipped as `String` in
  any release, so there is nothing to migrate.

### Features

* add auction enhancements (placementId, qualityScores, opaqueUserId) ([#120](https://github.com/Topsort/topsort.kt/issues/120)) ([b27faf2](https://github.com/Topsort/topsort.kt/commit/b27faf297e4029b7d9329e3117c7b075fb1e42dd))
* add enhanced event context fields ([#116](https://github.com/Topsort/topsort.kt/issues/116)) ([0c570fe](https://github.com/Topsort/topsort.kt/commit/0c570fef8aa230f2ceec2decbda9f1787f15c5c1))
* add Page model and PageView event tracking ([#115](https://github.com/Topsort/topsort.kt/issues/115)) ([29044aa](https://github.com/Topsort/topsort.kt/commit/29044aa5b3818bba7085e79602bb8f3b7c28ba6f))
* add response enhancements (campaignId, Asset.content) ([#122](https://github.com/Topsort/topsort.kt/issues/122)) ([457570f](https://github.com/Topsort/topsort.kt/commit/457570f823154bd5fdd4e02598a3b9447fbb9d89))


### Bug Fixes

* bound the event-type ordinal in EventEmitterWorker, with instrumented Cache and worker tests ([#124](https://github.com/Topsort/topsort.kt/issues/124)) ([d6b7de1](https://github.com/Topsort/topsort.kt/commit/d6b7de14c04b1d9c174594a704cb55697e976e70))
* give each event its own work unit so one failure can't silence the pipeline ([#159](https://github.com/Topsort/topsort.kt/issues/159)) ([417984b](https://github.com/Topsort/topsort.kt/commit/417984b8ce9eb948fcb3f51d75142f675a4a265b))
* handle explicit JSON null in getStringOrNull, getIntOrNull, getStringListOrNull ([#113](https://github.com/Topsort/topsort.kt/issues/113)) ([b6deb3f](https://github.com/Topsort/topsort.kt/commit/b6deb3f96c0b9faa8b40ec5f3614b8f890283531))
* make release-please actually bump the published version ([#162](https://github.com/Topsort/topsort.kt/issues/162)) ([7a29394](https://github.com/Topsort/topsort.kt/commit/7a29394c02ed240621dd30dc89e11393f172c7e7))
* recover events that were cached but never delivered ([#161](https://github.com/Topsort/topsort.kt/issues/161)) ([163d1bc](https://github.com/Topsort/topsort.kt/commit/163d1bc77f54c1360941f0af300919547f7a206e))
* report the real SDK version in the User-Agent ([#160](https://github.com/Topsort/topsort.kt/issues/160)) ([7bd520f](https://github.com/Topsort/topsort.kt/commit/7bd520f5170335b99f0e6eeaf72bd04f59b0f453))

## [2.0.1](https://github.com/Topsort/topsort.kt/compare/v2.0.0...v2.0.1) (2026-03-09)


### Bug Fixes

* add error logging in EventEmitterWorker ([#89](https://github.com/Topsort/topsort.kt/issues/89)) ([b67e84e](https://github.com/Topsort/topsort.kt/commit/b67e84ee4348174a68c0f0a432822e6d4bcdab2c))
* close HTTP connections on success and fix READ_TIMEOUT typo ([#80](https://github.com/Topsort/topsort.kt/issues/80)) ([8674799](https://github.com/Topsort/topsort.kt/commit/86747995de8760ee0fa039116a81ed5609417a78))
* distinguish 4xx from 5xx in EventEmitterWorker retry logic ([#95](https://github.com/Topsort/topsort.kt/issues/95)) ([8d1086e](https://github.com/Topsort/topsort.kt/commit/8d1086e98e058627ec02455f48552d81f36769f5))
* lower jvmTarget to 11 for broader consumer compatibility ([#90](https://github.com/Topsort/topsort.kt/issues/90)) ([4bc9094](https://github.com/Topsort/topsort.kt/commit/4bc9094f2c10cb026232567372e92aedcd7c0b2b))
* prevent duplicate impression reports and use proper logging in BannerView ([#79](https://github.com/Topsort/topsort.kt/issues/79)) ([ce6e31f](https://github.com/Topsort/topsort.kt/commit/ce6e31f7c30cfe404ec1b32584c4a4099323c590))
* prevent NPE crashes in event reporting and banner auctions ([#77](https://github.com/Topsort/topsort.kt/issues/77)) ([7dbddc0](https://github.com/Topsort/topsort.kt/commit/7dbddc07ce6cb660e3d06c573a6b1829668e365c))
* prevent race condition in Cache.nextRecordKey() ([#83](https://github.com/Topsort/topsort.kt/issues/83)) ([e1dee37](https://github.com/Topsort/topsort.kt/commit/e1dee37fb02acff80dabdcb6bc56b63fba3ce0d4))
* send events on any network and retry on transient failures ([#78](https://github.com/Topsort/topsort.kt/issues/78)) ([56bb9e5](https://github.com/Topsort/topsort.kt/commit/56bb9e5b71abfa8f244154bba63a7c8a13908f12))
* use **/ prefix in CODEOWNERS for recursive matching ([#87](https://github.com/Topsort/topsort.kt/issues/87)) ([0a6c3e5](https://github.com/Topsort/topsort.kt/commit/0a6c3e5863d2908b6bfa37e42e50ffe3e2b71234))
* use EncryptedSharedPreferences for token storage ([#93](https://github.com/Topsort/topsort.kt/issues/93)) ([968b632](https://github.com/Topsort/topsort.kt/commit/968b6324782f70196ffe54ec38645d701ac202f3))

## Changelog

All notable changes to this project will be documented in this file.

The format is loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
We follow the format used by [Open Telemetry](https://github.com/open-telemetry/opentelemetry-python/blob/main/CHANGELOG.md).

## Version 2.0.0 (2024-11-15)

### Added
- Comprehensive error handling with `AuctionError` sealed class for better error identification and processing
- Callback system for banner auctions:
  - `onError`: General error handling for all errors
  - `onAuctionError`: Specific auction error handling
  - `onNoWinners`: Callback for when an auction returns no winners
  - `onImageLoad`: Callback for successful banner image loading
- Testing utilities including mock implementations for AuctionsHttpService
- API for mocking services in tests: `setMockService` and `resetToDefaultService`

### Changed
- **BREAKING**: Upgraded minimum Java version to 17
- Improved coroutine implementation for auction requests using withContext
- Enhanced banner auction process with comprehensive error handling
- Made TopsortAuctionsHttpService accessible for testing with @VisibleForTesting annotation

### Fixed
- Multiple error propagation issues in banner auctions
- Streamlined null handling in auction responses

## Version 1.1.1 (2024-10-11)

### Added

- Added CD support by @anonvt in ([#28](https://github.com/Topsort/topsort.kt/pull/28))

## Version 1.1.0 (2024-09-12)

### Added

- Added support to Banners by @fcs-ts in ([#21](https://github.com/Topsort/topsort.kt/pull/21))

## Version 1.0.0-alpha.0 (2024-09-12)

### Added

- Added maven publishing configurations by @anonvt in ([#25](https://github.com/Topsort/topsort.kt/pull/25))
