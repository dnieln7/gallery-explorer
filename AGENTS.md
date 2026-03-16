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
| **Navigation**           | adrielcafe's Voyager                               | Use 1.1.0-beta03                           |
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
        * `.../presentation/composables/`: Reusable UI components.
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
