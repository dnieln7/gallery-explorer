# Detekt Levels

Use this reference when a finding is ambiguous during triage.

## Level 1

Classify a finding as Level 1 when the fix is formatting-only and does not change control flow, data flow, names,
visibility, or signatures.

Common examples:

- trailing commas
- indentation
- whitespace and spacing
- maximum line length caused by wrapping
- import ordering or package spacing
- other punctuation and basic formatting issues

## Level 2

Classify a finding as Level 2 when the fix is local, low-risk, and does not require a meaningful redesign.

Common examples:

- magic numbers moved to a named constant
- renaming a private symbol for clarity or rule compliance
- adjusting a visibility modifier
- removing an unused private property, parameter, or function
- simple simplifications where the behavior remains the same
- coroutine or suspend cleanups that do not change the intended control flow

Use the higher level if the same finding would force a public API change or a broader rewrite.

## Level 3

Classify a finding as Level 3 when the fix would require a broader refactor, extraction, state reshaping, or meaningful
signature changes.

Common examples:

- long parameter lists
- complex conditions
- large classes
- too many functions in a class or file
- nested block depth issues
- any fix that would likely move logic across files or layers

These findings always need a user decision before acting.

## Unclassified

Use Unclassified when the rule is unfamiliar, the report is unclear, or the safest remediation is not obvious from the
current context.

Common examples:

- rules with repo-specific or framework-specific tradeoffs
- rules that appear to conflict with the current architecture
- findings that may require suppressions, config changes, or generated-code exceptions
- findings where the impact on behavior is uncertain

When uncertain between Level 2 and Level 3, prefer Level 3. When uncertain between a known level and Unclassified,
prefer Unclassified.
