# Changelog

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