# Gallery Explorer

## Overview

**Gallery Explorer**: Media-only Android file explorer (Images/Video).
**Exclusions**: No audio-only files.
**Key UX**: Viewers must support horizontal swiping between files of the same type without exiting
to the folder view.

## Project tech stack

### General

| Category                 | Technology                                         | Notes                                      |
|:-------------------------|:---------------------------------------------------|:-------------------------------------------|
| **Language**             | Kotlin 2.0+                                        | Strict Kotlin; No Java.                    |
| **UI (App)**             | Compose (Material 3)                               | No XML layouts.                            |
| **Asynchrony**           | Coroutines & Flow                                  | Avoid RxJava or callbacks.                 |
| **DI**                   | Hilt                                               | Standard Hilt implementation.              |
| **Navigation**           | adrielcafe's Voyager with Hilt integration         | Use `1.1.0-beta03`                         |
| **Build System**         | Gradle KTS + Version Catalogs (libs.versions.toml) | Use located in `gradle/libs.versions.toml` |
| **Architecture Pattern** | MVI                                                | Action, State, Event                       |

### Data Persistence

* **Database**: Room
* **Migrations**:
    * Use **Room Auto-migrations** by default.
    * **AutoMigrationSpec** is only required for complex changes (rename/delete).
* **Schemas**: Located at `app/schemas`.
* **Key-Value**: DataStore (Preferences).

### Testing

* **Framework**: Junit 4
* **Assertions**: Kluent
* **Mocking**: Mockk
    * **Rule**: Use relaxed mocks.
* **Flows**: Turbine
* **Asynchrony**: Jetbrains's Coroutines test
* **Architecture**: Konsist
* **Coverage**: Kover

All tests are executable on the JVM, Robolectric is used for tests that require components of the
Android framework like Context. Test are located in `app/src/test`.

Test functions must follow the pattern `GIVEN ... WHEN ... THEN ...`:

* GIVEN: Describes the behavior of the test scenario.
* WHEN: Describes the action or event that triggers the test scenario.
* THEN: Describes the expected result or outcome of the test scenario.

Test functions must use backticks to improve readability like:

```kotlin
fun `This function name is readable`() {

}
```

## Project file structure

The project contains a single module: `app`.

### app module

Contains the code of the application, the sources are in `app/src/main/java/xyz/dnieln7/galleryex`.

#### Structure

* `core/`: Shared logic and utility functions that can be used in any feature.
    * `core/domain/`: Interfaces, use cases, models, and utilities globally used by `app`.
    * `/core/framework/`: Global Android platform specific utilities like WorkManager, Timber,
      Context, etc.
    * `core/presentation/`: Global UI components, Themes, UI Utils.
* `di/`: Hilt dependency injection modules
* `feature/`: Individual logic units that represent a single feature of the app, every unit contains
  a `presentation` package for the Screen/ViewModels and a `domain` package for the business logic
  and models.
    * **Rule**: Every presentation package must split into:
        * `.../presentation/screen/`: Screens and ViewModels.
        * `.../presentation/component/`: Reusable UI components.
    * Features can have it's own `.../domain/` and `.../framework/` packages that are not used by
      other features.
* `main/`: Entry point components; MainActivity.
* `GalleryExplorerApplication.kt`: Global app initialization.

## Project Coding standards

### Naming conventions

* Always use CamelCase
* Interface implementations must have the prefix `Default` for example, the implementation of
  `AppLogger` will be `DefaultAppLogger`
* Database models must end with `DbModel`

### Documentation & Metadata

* **Format**: Use KDoc (/** ... */) for all public classes and functions.
* **Tags**: Use `@property`, `@return`, `@deprecated`, etc to maintain high readability.

### UI & Architecture (MVI + Voyager)

**The Destination Contract**

The [Feature]ScreenDestination : Screen is the Orchestrator. It is strictly responsible for:

* **Dependency Injection**: Obtaining the ViewModel via `getViewModel<T>()`.
* **State Collection**: Transforming `uiState` into a Compose State via
  `collectAsStateWithLifecycle()`.
* **Event Handling**: Observing the `events` Flow and executing side-effects (Toasts, Dialogs).
* **Lifecycle Mapping**: Using `LifecycleEventEffect` to bridge platform events (e.g., `ON_RESUME`)
  to `onAction` calls.
* **Navigation Logic**: Defining the lambdas that call `navigator.push/replace`. UI Composables must
  never see the Navigator.

**The View Contract**

The `[Feature]Screen` (Stateless) is the Renderer. It is strictly responsible for:

* **Layout**: Using `Scaffold`, `Column`, `LazyColumn`, etc.
* **Interactions**: Passing user clicks back to the Orchestrator via `onAction`.
* **Visual States**: Mapping the `[Feature]State` to specific UI branches.

**MVI Pattern Definitions**

* `[Feature]State`: Immutable Data class representing the entire screen.
* `[Feature]Action`: Sealed interface for user intents (e.g., `OnRefresh`, `OnRequestAccess`).
* `[Feature]Event`: Sealed interface for one-time commands (e.g., `ShowSnackbar`, `Logout`).

**ViewModel Contract**

* **Entry Point**: A single entry point `fun onAction(action: [Feature]Action)`.
* **State Output**: `private val _uiState = MutableStateFlow([Feature]State())` expose as a
  `StateFlow` via `asStateFlow()`. Update via `_uiState.update { ... }`.
* **Event Output**: `private val _events = Channel<[Feature]Event>()` expose as a `Flow` via
  `receiveAsFlow()`. Update via `_events.send(...)`.

### Compose Standards

* **Modifiers**: First parameter of any public/internal Composable must be modifier:
  `Modifier = Modifier`.
* **Theming**: Use `MaterialTheme.colorScheme` and `stringResource`. No hardcoded values.
* **Components**: Use project-specific atoms (e.g., `GalleryButtonPrimary`, `VerticalSpacer`).
* **Previews**: Mandatory for all stateless `[Feature]Screen` composables using
  `GalleryExplorerTheme`.

## Project Rules

1. Never deviate from the Material 3 guidelines.
2. Composables that represent an specific functionality or state must be separated into their own files like @GalleryButtonPrimary.kt
3. Shapes must be created in @Shape.kt
4. Composables that are only needed in an specific feature must be in [feature]/presentation/component otherwise they must be in @core/presentation/component
