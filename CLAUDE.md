# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

**Java 25+ compatibility**: Use Android Studio's JDK:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

```bash
./gradlew build                    # Build project
./gradlew assembleDebug            # Build debug APK
./gradlew installDebug             # Install on connected device
./gradlew test                     # Run unit tests
./gradlew connectedAndroidTest     # Run instrumented tests
./gradlew clean                    # Clean project
```

## Architecture

Clean Architecture with MVVM pattern. Code is in `app/src/main/java/com/devlosoft/megaposmobile/`.

### Layer Structure

| Layer | Directory | Purpose |
|-------|-----------|---------|
| Presentation | `presentation/` | Compose UI, ViewModels, Navigation |
| Domain | `domain/` | Business models, repository interfaces, use cases |
| Data | `data/` | Repository implementations, Retrofit APIs, Room DB, DataStore |
| Core | `core/` | Common utilities (`Resource`), printer services |
| DI | `di/` | Hilt modules |

### Key Patterns

**ViewModel State Pattern**: ViewModels expose `StateFlow<State>` and handle events via `onEvent(Event)`:
```kotlin
// State is immutable data class
data class BillingState(val isLoading: Boolean = false, ...)

// Events are sealed class
sealed class BillingEvent {
    data class ArticleSearchQueryChanged(val query: String) : BillingEvent()
}

// ViewModel pattern
class BillingViewModel : ViewModel() {
    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    fun onEvent(event: BillingEvent) { ... }
}
```

**Resource wrapper** (`core/common/Resource.kt`): Wraps API responses with Loading/Success/Error states. Repositories return `Flow<Resource<T>>`.

**Repository Pattern**: Interfaces in `domain/repository/`, implementations in `data/repository/`, bindings in `di/RepositoryModule.kt`.

### Dependency Injection (Hilt)

- `NetworkModule.kt`: Retrofit, OkHttp, all API interfaces
- `DatabaseModule.kt`: Room database, DAOs
- `RepositoryModule.kt`: Repository bindings

### Navigation

Screens defined in `presentation/navigation/Screen.kt` as sealed class. Routes with parameters use pattern: `Screen("route/{param}")` with `createRoute(param)` function.

### Adding New Features

See `IMPLEMENTATION_GUIDE.md` for complete walkthrough. Key steps:
1. DTOs in `data/remote/dto/` with `toDomain()` mapper
2. Domain model in `domain/model/`
3. API interface in `data/remote/api/`
4. Repository interface in `domain/repository/`
5. Repository implementation in `data/repository/`
6. Register API in `NetworkModule.kt`, repository in `RepositoryModule.kt`
7. ViewModel + Screen in `presentation/`

### Printer Integration

Printer services in `core/printer/`:
- `PrinterManager`: Main entry point, handles both IP and Bluetooth printers
- `PrinterDriverFactory`: Creates appropriate driver based on printer model
- Configuration stored in Room via `ServerConfigDao`

### API Configuration

- Base URL: `BuildConfig.API_BASE_URL` (set in `app/build.gradle.kts`)
- Debug: `http://10.0.2.2:6060/pos-api/v1/` (emulator localhost)
- Release: Change `YOUR_SERVER_IP` placeholder
- JWT auth via `AuthInterceptor`, token stored in `SessionManager` (DataStore)
- `DEVELOPMENT_MODE` BuildConfig flag skips printer connectivity test
