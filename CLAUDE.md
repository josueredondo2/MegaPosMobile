# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MegaPosMobile is a native Android POS (Point of Sale) application built in Kotlin for the MegaSuper retail chain. It runs on PAX A920 terminals and standard Android devices, integrating with dataphones, printers, and barcode scanners.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (signed)
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests on device/emulator
./gradlew build                  # Full build (compile + test + lint)
```

## Architecture

Clean Architecture with three layers, all under `app/src/main/java/com/devlosoft/megaposmobile/`:

- **presentation/** — Jetpack Compose UI + MVVM ViewModels. Each feature has its own package (login, home, billing, process, configuration, advancedoptions, todaytransactions). Navigation uses type-safe routes via Kotlin Serialization (defined in `presentation/navigation/`).
- **domain/** — Pure Kotlin business logic. Repository interfaces in `domain/repository/`, domain models in `domain/model/`, use cases in `domain/usecase/`.
- **data/** — Implementation layer. `data/remote/api/` has Retrofit interfaces (AuthApi, CustomerApi, TransactionApi, PaymentApi, FelApi, CatalogApi, AuditApi, SystemApi). `data/local/` has Room database (`MegaPosDatabase`, currently at schema version 8) and DataStore preferences (`SessionManager`). `data/repository/` has repository implementations.

Supporting packages:
- **di/** — Hilt modules (NetworkModule, RepositoryModule, DatabaseModule)
- **core/** — Peripheral drivers, session management, utilities
  - `core/printer/` — PrinterManager with Zebra ZQ511 Bluetooth driver
  - `core/dataphone/` — DataphoneManager routing between embedded PAX (via BAC Credomatic `kpinvocacion.1.1.0.aar`) and HTTP-based dataphones
  - `core/scanner/` — ScannerManager with Zebra and PAX drivers (factory pattern)
  - `core/session/` — InactivityManager for auto-logout
  - `core/common/` — Resource wrapper, ApiConfig, Constants

## Key Technical Details

- **Language**: Kotlin 2.3.0, Java 17 target
- **UI**: Jetpack Compose (BOM 2025.12.01) with Material Design 3
- **DI**: Hilt 2.57.2 (uses KSP, not kapt)
- **Networking**: Retrofit 2.11.0 + OkHttp 5.0.0-alpha.14 with custom `AuthInterceptor` for JWT
- **Database**: Room 2.8.4 with incremental migrations (1→8)
- **State**: StateFlow in ViewModels; UI state classes per feature (e.g., `BillingState`, `ProcessState`)
- **Navigation**: Navigation Compose 2.9.6 with type-safe routes using `@Serializable` data objects
- **Min SDK**: 24 (Android 7.0), Target/Compile SDK: 36
- **Two API base URLs**: `pos-api/v1/` (main) and `fel-api/v1/` (electronic invoicing), configured via BuildConfig

## Peripheral Integration

Peripherals use a driver/factory pattern (`DataphoneDriver`, `ScannerDriver` interfaces) allowing runtime switching:

- **PAX A920 embedded dataphone**: Launches external KinPos app via intent (`com.kinpos.BASEA920`), parses responses through `PaxBacDriver` with 80+ response codes
- **HTTP dataphone**: `HttpDataphoneService` with configurable drivers
- **Printers/Scanners**: Manager classes with Bluetooth discovery and connection lifecycle

## Conventions

- ViewModels expose `StateFlow<*State>` and receive events via sealed classes or direct function calls
- API responses are wrapped in `Resource<T>` (Success/Error/Loading) from `core/common/`
- Room migrations are defined inline in `MegaPosDatabase.kt`
- The billing flow uses a nested navigation graph so screens share a single `BillingViewModel`
- Version catalog in `gradle/libs.versions.toml` manages all dependency versions
