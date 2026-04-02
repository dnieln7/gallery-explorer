---
name: mvi-screen-viewmodel
description: Create or update a ViewModel-backed MVI screen for this Android repository. Use when the assistant needs to add or modify a feature screen that must follow the Gallery Explorer MVI + Voyager contract, including [Feature]ScreenDestination orchestration, stateless [Feature]Screen rendering, preview, [Feature]ViewModel, required [Feature]State/[Feature]Action/[Feature]Event files in domain/model, and JVM ViewModel tests with Turbine, coroutines test, MockK, and Kluent. Do not use for light screens that do not have a ViewModel.
---

# MVI Screen ViewModel

Create the full ViewModel-backed MVI screen slice for this repository and keep it aligned with `AGENTS.md`.

Read these bundled references before writing code:

- `references/example/` for screen, ViewModel, and model shape
- `references/example-test/` for ViewModel test style and assertion order

Treat `AGENTS.md` as the source of truth when it conflicts with the examples.

## Generation Checklist

Before finishing, confirm all of these are true:

- create or update `[Feature]Screen.kt` with destination, stateless screen, and preview in one file
- create or update `[Feature]ViewModel.kt`
- create or update `[Feature]State.kt`, `[Feature]Action.kt`, and `[Feature]Event.kt`
- keep navigation and side effects in the destination, never in the stateless screen
- keep business orchestration in the ViewModel, never in the stateless screen
- expose `uiState` from `MutableStateFlow(...).asStateFlow()`
- scaffold `_events` plus `events` in the ViewModel even when the event contract is empty
- collect events in the destination only when there are real events to handle
- move large or reusable UI blocks into `presentation/component/`
- add KDoc to public classes and public functions
- add a stateless screen preview with `GalleryExplorerTheme`
- add or update `[Feature]ViewModelTest.kt`
- verify success paths, error paths, collaborator calls, and ordered state or event emissions

## Required Outputs

Create or update these files for the feature:

- `feature/<feature>/presentation/screen/[Feature]Screen.kt`
- `feature/<feature>/presentation/screen/[Feature]ViewModel.kt`
- `feature/<feature>/domain/model/[Feature]State.kt`
- `feature/<feature>/domain/model/[Feature]Action.kt`
- `feature/<feature>/domain/model/[Feature]Event.kt`
- `app/src/test/.../[Feature]ViewModelTest.kt`

Do not create feature error files from this skill. Error modeling already belongs to the repository rules in
`AGENTS.md`.

## Screen File Contract

Keep `[Feature]ScreenDestination`, the stateless `[Feature]Screen`, and the preview in the same `[Feature]Screen.kt`
file. Apply this skill to one screen variant at a time. If a feature has multiple variants, each variant may have its
own screen file.

Use `[Feature]ScreenDestination` as the orchestrator. It may:

- obtain the ViewModel with `getViewModel<T>()`
- collect `uiState` with `collectAsStateWithLifecycle()`
- map lifecycle or screen effects to `onAction(...)`
- define navigation lambdas
- collect events only when the feature has real events to handle

Do not expose `Navigator` to the stateless screen.

Use the stateless `[Feature]Screen` as the renderer. It must:

- receive state plus callbacks only
- map visual state to UI branches
- send user interactions back through `onAction`

`[Feature]Screen` and other root-like composables may omit `modifier: Modifier = Modifier` as the
first parameter. Follow the standard AGENTS modifier rule for non-root composables.

When the screen file starts carrying large state-specific UI blocks or reusable pieces, move those
pieces to `feature/<feature>/presentation/component/`.

## Model Contract

Always create all three MVI model files, even when a feature has no one-time side effects yet:

- `[Feature]State`: immutable data class for the whole screen state
- `[Feature]Action`: sealed interface for user intents
- `[Feature]Event`: sealed interface for one-time effects

An empty event contract is allowed when the feature does not emit events yet.

## ViewModel Contract

Use a single public entry point:

```kotlin
fun onAction(action: FeatureAction) {
    when (action) {
        // ...
    }
}
```

Expose state with `MutableStateFlow(...).asStateFlow()` and update it with `.update { ... }`.

Always scaffold the event channel even when the event contract is currently empty:

```kotlin
private val _events = Channel<FeatureEvent>()
val events = _events.receiveAsFlow()
```

Keep orchestration in the ViewModel. Call repositories, use cases, or controllers from there, not
from the stateless screen.

Prefer small private helper functions such as `onRefresh()` or `onSubmit()` when they make the action dispatch easier
to scan.

## Initial Load Trigger

Do not guess the initial load trigger when the correct choice is unclear.

When the feature requirements do not clearly imply the trigger, stop and ask the user to choose one
before generating code. Offer a short flat list with concrete options such as:

- ViewModel `init`
- `LifecycleEventEffect(...)`
- `LaunchedEffect(...)`
- no automatic load

Use the simplest option that matches the requirement when the trigger is obvious, and mirror the chosen trigger in the
tests.

## Tests

Write JVM ViewModel tests with:

- JUnit 4
- `runTest`
- `StandardTestDispatcher`
- Turbine
- relaxed MockK mocks
- Kluent assertions

Test names must use the repository format:

```kotlin
fun `GIVEN ... WHEN ... THEN ...`() {
}
```

At minimum, cover:

- success path for the initial or primary action
- error path for the initial or primary action
- success path for user-driven actions
- error path for user-driven actions
- ordered state emissions
- ordered event emissions when events exist
- verification that the expected collaborator functions are called in the expected order

When the feature trigger is automatic, assert the emissions caused by that automatic trigger instead of skipping the
behavior.

When the feature currently has an empty event contract, skip event collection assertions but still verify state flow
behavior and collaborator calls.

## Repository Rules To Enforce

Apply these repository-specific rules every time:

- add KDoc to public classes and public functions
- keep file-local declarations private
- use trailing commas
- avoid expression bodies
- keep constants at the end of the file
- use Material 3 plus repository UI components where appropriate
- add a preview for the stateless screen with `GalleryExplorerTheme`

## Non-Goals

Do not use this skill for screens that do not have a ViewModel. Those screens may still follow the
destination plus screen plus preview structure, but they are outside this skill's scope.
