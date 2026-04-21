# Changelog

## [3.0.0](https://github.com/Topsort/topsort.kt/compare/v2.0.1...v3.0.0) (2026-04-21)


### ⚠ BREAKING CHANGES

* deviceType, channel, clickType fields now use enums instead of String

### Features

* add auction enhancements (placementId, qualityScores, opaqueUserId) ([#120](https://github.com/Topsort/topsort.kt/issues/120)) ([b27faf2](https://github.com/Topsort/topsort.kt/commit/b27faf297e4029b7d9329e3117c7b075fb1e42dd))
* add enhanced event context fields ([#116](https://github.com/Topsort/topsort.kt/issues/116)) ([0c570fe](https://github.com/Topsort/topsort.kt/commit/0c570fef8aa230f2ceec2decbda9f1787f15c5c1))
* add Page model and PageView event tracking ([#115](https://github.com/Topsort/topsort.kt/issues/115)) ([29044aa](https://github.com/Topsort/topsort.kt/commit/29044aa5b3818bba7085e79602bb8f3b7c28ba6f))
* add response enhancements (campaignId, Asset.content) ([#122](https://github.com/Topsort/topsort.kt/issues/122)) ([457570f](https://github.com/Topsort/topsort.kt/commit/457570f823154bd5fdd4e02598a3b9447fbb9d89))


### Bug Fixes

* handle explicit JSON null in getStringOrNull, getIntOrNull, getStringListOrNull ([#113](https://github.com/Topsort/topsort.kt/issues/113)) ([b6deb3f](https://github.com/Topsort/topsort.kt/commit/b6deb3f96c0b9faa8b40ec5f3614b8f890283531))

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
