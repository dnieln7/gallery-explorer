<!-- Sync Impact Report
  Version change: (template) → 1.0.0
  Added sections:
    - I. Code Quality
    - II. Testing Standards
    - III. User Experience Consistency
    - IV. Performance Requirements
    - Architecture & MVI Contract
    - Development Workflow & Quality Gates
    - Governance
  Removed sections: none (template had no real content)
  Templates updated:
    - .specify/templates/plan-template.md ✅ Constitution Check hints updated
    - .specify/templates/spec-template.md ✅ no changes required
    - .specify/templates/tasks-template.md ✅ no changes required
  Deferred TODOs: none
-->

# Gallery Explorer Constitution

## Core Principles

### I. Code Quality

All code MUST be written in Kotlin 2.0+ with no Java source files in the `app` module.
Every public class and public function MUST have a KDoc block (`/** ... */`) using
`@property`, `@return`, and `@deprecated` tags where applicable.

Naming MUST follow these exact rules:
- CamelCase everywhere; no exceptions.
- Interface implementations MUST use the `Default` prefix (e.g., `DefaultAppLogger`).
- Database models MUST end with `DbModel`.
- Constants MUST use `SCREAMING_SNAKE_CASE` and MUST be placed at the end of the file.
- Magic numbers are PROHIBITED; every numeric literal that is not `0` or `1` MUST be
  extracted to a named `const val`.

Syntax MUST meet these requirements:
- Functions MUST always use a body block; the expression-body shorthand `fun foo() = …`
  is PROHIBITED.
- Trailing commas MUST appear on all multi-line parameter, argument, and collection
  literals.
- Files MUST end with a newline character (`\n`).
- Class bodies MUST NOT start with a blank line.
- Wrapper functions that only delegate to another call without adding transformation,
  validation, or reusable domain behavior MUST NOT exist; inline those call sites instead.
- Any function, constant, or class used exclusively within its own file MUST be `private`.

Formatting is enforced by Detekt and KTLint (`config/detekt/config.yml`, `.editorconfig`).
Detekt baseline files (`**/detekt/baseline.xml`) MUST NEVER be modified, and Detekt warnings
MUST NEVER be suppressed without explicit written permission.

Shared contracts, models, events, and errors used by more than one feature MUST live under
`core/`. Domain contracts and pure models belong in `core/domain/<area>/`; Android/framework
implementations in `core/framework/<area>/`; Compose/UI helpers in `core/presentation/<area>/`.
These three categories MUST NOT be co-located in the same file.

### II. Testing Standards

Every new class with business or presentation logic MUST be accompanied by JVM-executable
unit tests in `app/src/test`. Robolectric MUST be used only when an Android framework
component (e.g., `Context`) is genuinely required; it is PROHIBITED for pure Kotlin or
ViewModel-only logic.

All test functions MUST:
- Use backtick names for readability.
- Follow the `GIVEN … WHEN … THEN …` pattern exactly.

The testing stack MUST be:
- **Framework**: JUnit 4.
- **Assertions**: Kluent; raw JUnit `assert*` calls are PROHIBITED.
- **Mocking**: MockK with relaxed mocks (`relaxedMockk<T>()`); non-relaxed mocks are
  PROHIBITED unless the test explicitly needs strict interaction verification.
- **Flow/Channel testing**: Turbine; manual `collect` in coroutine scopes is PROHIBITED
  for asserting emissions.
- **Coroutine scheduling**: Jetbrains Coroutines Test (`runTest`); `MainDispatcherRule`
  MUST be applied to any test that touches `Dispatchers.Main`.
- **Architecture invariants**: Konsist.

ViewModel tests MUST verify state transitions by collecting `uiState` with Turbine and
MUST verify event emissions by collecting `events` with Turbine. Tests MUST cover every
branch of `onAction(…)`.

### III. User Experience Consistency

Gallery Explorer is a media-only app. It MUST display images and videos. Audio-only files
MUST NOT be surfaced anywhere in the UI.

Navigation between files of the same media type within a folder MUST produce a TikTok-style
vertical-swipe experience using `VerticalPager`. Any alternative in-folder navigation
mechanism is PROHIBITED.

All UI MUST conform strictly to Material 3 guidelines:
- Hardcoded color values are PROHIBITED; `MaterialTheme.colorScheme` MUST always be used.
- Hardcoded string literals in UI are PROHIBITED; `stringResource(…)` MUST always be used.
- Custom shapes MUST be defined in `core/presentation/theme/Shape.kt`.

Composable functions MUST follow these rules:
- The first parameter of every non-root Composable MUST be `modifier: Modifier = Modifier`.
  Root-level `[Feature]Screen` composables may omit this.
- A `@Preview` annotated in `GalleryExplorerTheme` is MANDATORY for every stateless
  `[Feature]Screen` composable and every component with its own file.
- Composables exclusively used within their own file MUST be `private`.
- Shared UI components MUST live in `core/presentation/component/`; feature-specific
  components MUST live in `[feature]/presentation/component/`.

Error messages surfaced to the user MUST use `UIText` so that all strings remain testable
and localizable. Hardcoded strings passed directly to Toast or Snackbar are PROHIBITED.

### IV. Performance Requirements

Video playback MUST use `ExoPlayer` (Media3). Any other video decoding mechanism is
PROHIBITED. The `ExoPlayer` instance MUST be owned by the ViewModel to survive
configuration changes.

Loading states for video MUST be driven by `Player.Listener.onRenderedFirstFrame()`.
Setting `isVideoReady = true` before this callback fires is PROHIBITED.

The UI rendering target is 60 fps. Position polling for video progress MUST use
`collectLatest` to stop automatically when playback is paused or scrubbing is active,
avoiding unnecessary main-thread work.

Polling intervals and auto-hide delays MUST be defined as named constants
(e.g., `CONTROLS_AUTO_HIDE_DELAY_MS`, `POSITION_POLLING_INTERVAL_MS`). Their values
MUST NOT be changed without profiling evidence.

## Architecture & MVI Contract

All feature screens MUST split responsibilities across exactly two Composable boundaries:

**The Destination (`[Feature]ScreenDestination : Screen`)** is the orchestrator:
1. Obtains the ViewModel via `getViewModel<T>()`.
2. Collects `uiState` as Compose State via `collectAsStateWithLifecycle()`.
3. Observes the `events` Flow and executes side-effects (Toasts, Dialogs, navigation).
4. Maps lifecycle events to `onAction(…)` via `LifecycleEventEffect`.
5. Defines all navigation lambdas (`navigator.push(…)`, `navigator.replace(…)`).

The Navigator MUST NEVER be passed to or referenced by the stateless Screen.

**The Screen (`[Feature]Screen`, `private`)** is the stateless renderer: layout only,
plus routing user interactions via `onAction: ([Feature]Action) -> Unit`.

**MVI triads** (`[Feature]State`, `[Feature]Action`, `[Feature]Event`) MUST be located in
`[feature]/domain/model`. MUST NOT be defined inside ViewModel or presentation files.

**ViewModel shape**:
- Single entry point: `fun onAction(action: [Feature]Action)`.
- State: `private val _uiState = MutableStateFlow([Feature]State())` exposed as
  `val uiState = _uiState.asStateFlow()`. Updates via `_uiState.update { … }`.
- Events: `private val _events = Channel<[Feature]Event>()` exposed as
  `val events = _events.receiveAsFlow()`. Sent via `_events.send(…)`.

**Error handling**: All repositories, use cases, and controllers MUST wrap return types in
`Either<Error, T>` (Arrow-kt). `Either.catch { … }.mapLeft { it.toError() }` MUST be used;
bare try-catch blocks are PROHIBITED. ViewModels MUST use `fold(…)` to dispatch error
events. Feature errors MUST implement `core/domain/error/Error`. Shared errors belong in
`core/domain/error/`; their UI translations in `core/presentation/error/`.

## Development Workflow & Quality Gates

Every feature branch MUST pass these gates before merging:

1. **Detekt + KTLint**: Zero new violations; `baseline.xml` MUST NOT be modified.
2. **JVM Unit Tests**: All tests in `app/src/test` pass; no tests skipped without
   documented justification.
3. **Previews compile**: Every mandatory `@Preview` compiles and renders without errors.
4. **MVI contract audit**: Each new screen has a `ScreenDestination` orchestrator, a
   `private` stateless Screen, and State/Action/Event files in `[feature]/domain/model`.
5. **KDoc coverage**: All new public classes and functions have KDoc.

New dependencies MUST be declared in `gradle/libs.versions.toml`. Direct version strings
in `build.gradle.kts` are PROHIBITED.

Database schema changes MUST use Room Auto-migrations by default. `AutoMigrationSpec` is
only acceptable for renames or deletes. Schema files in `app/schemas` MUST be committed
alongside migrations.

## Governance

This constitution supersedes all local conventions, personal preferences, and ad-hoc
agreements. Where a conflict exists between this document and any other source, this
constitution takes precedence.

**AGENTS.md** is the runtime development guidance document. It provides the detailed
technical rules, code examples, and structural conventions that implement the principles
declared here. All contributors MUST read and follow AGENTS.md for day-to-day decisions.

Amendments to this constitution MUST:
1. Be documented with the change rationale.
2. Be ratified by the project maintainer.
3. Include a migration note if existing code must be updated.
4. Increment the version number and update the **Last Amended** date.

All pull requests and code reviews MUST verify compliance with this constitution and
AGENTS.md. Complexity that deviates from these principles MUST be explicitly justified
in the PR description with measurable benefit.

**Versioning policy**:
- MAJOR: Backward-incompatible governance/principle removals or redefinitions.
- MINOR: New principle or section added or materially expanded.
- PATCH: Clarifications, wording, or typo fixes.

**Version**: 1.0.0 | **Ratified**: 2026-05-17 | **Last Amended**: 2026-05-17
