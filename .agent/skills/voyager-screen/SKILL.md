---
name: voyager-screen
description: Create or update a simple non-ViewModel Voyager screen for this Android repository. Use when the assistant needs to add or modify a lightweight screen that should follow the Gallery Explorer screen structure with [Feature]ScreenDestination, a private stateless [Feature]Screen, and a preview in the same file. Do not use when the screen needs a ViewModel, MVI state models, event handling, or broader state orchestration.
---

# Voyager Screen

Create the simple Voyager screen structure for this repository and keep it aligned with `AGENTS.md`.

Treat `AGENTS.md` as the source of truth when it conflicts with the examples.

## Generation Checklist

Before finishing, confirm all of these are true:

- create or update `[Feature]Screen.kt`
- keep `[Feature]ScreenDestination`, the private stateless `[Feature]Screen`, and the preview in the same file
- keep navigation logic in the destination, never in the stateless screen
- pass only the data and callbacks the stateless screen needs
- move large or reusable child UI into `presentation/component/`
- add KDoc to public classes and public functions
- add a stateless screen preview with `GalleryExplorerTheme`
- avoid introducing a ViewModel or MVI files

## Required Output

Create or update:

- `feature/<feature>/presentation/screen/[Feature]Screen.kt`

Use one screen file per screen variant. If a feature has multiple simple variants, each variant may
have its own screen file.

## Screen File Contract

Keep `[Feature]ScreenDestination`, the private stateless `[Feature]Screen`, and the preview in the
same `[Feature]Screen.kt` file.

Use `[Feature]ScreenDestination` as the orchestrator. It may:

- receive Voyager navigation arguments
- adapt complex arguments into feature-friendly values when that reduces boilerplate
- obtain the current navigator
- define navigation lambdas
- pass only the required data and callbacks into the stateless screen

Keep simple arguments such as ids or other small primitives as-is. Only add mapping helpers when
they make the file simpler.

Do not expose `Navigator` to the stateless screen.

Use the stateless `[Feature]Screen` as the renderer. It must:

- receive plain data and callbacks
- render the layout
- delegate user interactions through callbacks

`[Feature]Screen` and other root-like composables may omit `modifier: Modifier = Modifier` as the
first parameter. Follow the standard AGENTS modifier rule for non-root composables.

File-local helper functions are allowed when they reduce boilerplate and are only used by the
screen file.

Keep the screen focused on straightforward UI structure. If the request starts requiring broader
state ownership or behavioral orchestration, this skill is no longer the right fit.

## Component Boundary

When the screen file starts carrying large visual sections or reusable UI pieces, move those pieces
to `feature/<feature>/presentation/component/`.

Keep the screen file focused on:

- destination wiring
- screen layout
- preview
- small file-local helpers

## Repository Rules To Enforce

Apply these repository-specific rules every time:

- add KDoc to public classes and public functions
- keep file-local declarations private
- use trailing commas
- avoid expression bodies
- keep constants at the end of the file
- use Material 3 plus repository UI components where appropriate
- add a preview for the stateless screen with `GalleryExplorerTheme`

## Escalation Rule

If the screen starts needing non-trivial state orchestration, one-time events, or a ViewModel,
stop and use `mvi-screen-viewmodel` instead of stretching this skill beyond its scope.

## Non-Goals

Do not use this skill to create:

- a ViewModel
- `State`, `Action`, or `Event` files
- ViewModel tests
- broader state-management patterns
