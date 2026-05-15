---
name: detekt-fixer
description: Detekt workflow for this Android repository. Use when the assistant needs to run Detekt, inspect app/build/reports/detekt.md or app/build/reports/detekt.html, fix Detekt issues, triage Detekt findings, or clean up static analysis warnings. Classify findings into Level 1, Level 2, Level 3, or Unclassified, fix Level 1 and Level 2 issues when safe, and ask the user before any Level 3, Unclassified, suppression, or baseline decision.

---

# Detekt Fixer

## Overview

Run Detekt for the `app` module, classify findings with this project policy, fix the issues that are safe to
address directly, and escalate anything that requires a structural refactor or a baseline decision.

Use the bundled script for execution and read the bundled reference when a finding is ambiguous:

- `scripts/run_detekt.sh`
- `references/detekt-levels.md`

## Workflow

1. Confirm the current workspace is `/Users/dniel/Documents/projects-android/GalleryExplorer`.
2. Run `scripts/run_detekt.sh` from the repository root.
3. Read `app/build/reports/detekt.md` first. Use `app/build/reports/detekt.html` only if the markdown report is missing
   or unclear.
4. Inspect the reported files and classify every finding into Level 1, Level 2, Level 3, or Unclassified using
   `references/detekt-levels.md`.
5. Fix Level 1 findings directly.
6. Fix Level 2 findings directly when the change is local, low-risk, and does not alter the intended behavior.
7. Rerun Detekt after each batch of fixes until only Level 3 or Unclassified findings remain, or until the report is
   clean.
8. Present Level 3 and Unclassified findings in a short decision list that includes rule name, file, why it was
   classified that way, and the available actions.

## Classification Policy

### Level 1

Treat findings as Level 1 when they are limited to punctuation, indentation, whitespace, wrapping, trailing commas, line
length, or other basic formatting that does not require a logic change.

Fix Level 1 findings without asking for approval.

### Level 2

Treat findings as Level 2 when they require a minor code change but do not require a meaningful refactor. Common
examples include magic numbers, renaming, visibility modifiers, unused declarations, and similarly contained cleanups.

Fix Level 2 findings without asking for approval when the intent is clear and the change is low risk.

### Level 3

Treat findings as Level 3 when they require a major refactor, structural rewrite, API reshaping, or broader design
changes. Common examples include parameter count, complexity, too many functions, large classes, and deep nesting.

Do not fix or baseline Level 3 findings unilaterally. List them and ask the user whether to:

- fix the issue, or
- send the issue to the baseline

### Unclassified

Treat findings as Unclassified when they do not fit the first three levels cleanly, when the correct remediation is
unclear, or when the risk of behavior changes is not obvious.

Do not fix or baseline Unclassified findings unilaterally. List them and ask the user whether to:

- fix the issue, or
- send the issue to the baseline

## Repository Constraints

- Run `:app:detekt`; do not substitute a different module unless the user asks.
- Prefer code fixes over suppression or baseline changes.
- Never modify `app/detekt/baseline.xml` unless the user explicitly approves that specific baseline action.
- Never add `@Suppress`, detekt config exclusions, or rule disables unless the user explicitly approves them.
- Respect the repository coding rules in `AGENTS.md` while fixing issues, especially KDoc, visibility, Compose
  structure, and trailing comma expectations.
- Keep fixes consistent with the existing architecture. Do not use Detekt cleanup as an excuse to make unrelated feature
  changes.

## Output Expectations

After each Detekt pass, report:

- the command that was run,
- whether Detekt passed or failed,
- how many findings were placed in each level,
- which Level 1 and Level 2 findings were fixed,
- which Level 3 and Unclassified findings still need a user decision

When asking for decisions, use a compact flat list. For each finding, include the rule, the file, the reason it is Level
3 or Unclassified, and the two allowed actions: `Fix` or `Send to baseline`.
