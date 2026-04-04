---
summary: How InspectionSuppressor works — the // noinspection comment mechanism and what we test
updated: 2026-04-04
relates: [architecture]
---

## The problem

When a language is injected into a string (e.g. SQL), IntelliJ inspections for
that language may flag problems (e.g. "no data source configured"). The user
needs a way to suppress these warnings. IntelliJ's standard mechanism is a
`// noinspection <InspectionId>` comment above the declaration.

Without an `InspectionSuppressor` registered for our language, IntelliJ doesn't
know how to generate or recognize these comments in ReScript files.

## The four-step mechanism

1. **An inspection flags a problem.** For example, the SQL inspection creates a
   `ProblemDescriptor` pointing to the PSI element.

2. **IntelliJ asks for suppress actions.** It calls
   `getSuppressActions(element, "SqlNoDataSourceInspection")` on our suppressor.
   We return quick fixes that can insert the comment.

3. **The user clicks "Suppress".** IntelliJ calls `applyFix()` on our fix, which
   inserts `// noinspection SqlNoDataSourceInspection` + newline above the
   top-level declaration.

4. **The inspection re-runs.** IntelliJ calls
   `isSuppressedFor(element, "SqlNoDataSourceInspection")` on our suppressor.
   We walk up to the top-level declaration, scan leading comments for the
   `// noinspection` pattern, and return `true` if found. The warning disappears.

## What our tests verify

Our tests are **unit tests on the suppressor class**, not integration tests of the
full IDE pipeline. We don't simulate an actual inspection firing. Instead:

- We call `isSuppressedFor()` directly on PSI elements we construct, verifying
  it recognizes (or doesn't recognize) the `// noinspection` comment.
- We call `getSuppressActions()` and verify it returns actions.
- We apply a suppress action via `applyFix()` with a synthetic
  `ProblemDescriptor` and verify the comment is inserted on the line above the
  declaration (not on the same line).

The `"SomeInspection"` string in our tests is an arbitrary name — the mechanism
is the same regardless of the actual inspection ID.

## Known limitations

- "Suppress for file" is not yet supported (would need inserting at file start).
- "Suppress all" works at declaration level but is basic.
