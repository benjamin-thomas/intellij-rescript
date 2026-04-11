---
summary: Ctrl+Shift+T jumps between src/Foo.res and test/Foo_test.res
created: 2026-04-11
---

# Go to Test — file-level navigation

## Goal
Wire up Ctrl+Shift+T so the user can jump from a source file to its test
file (or vice versa) using the ReScript naming convention.

## Context
IntelliJ ships `GotoTestOrCodeHandler` for exactly this purpose. The
extension point is `testFinder` (`com.intellij.testFinder`) which returns a
`TestFinder` producing `findClassesForTest` / `findTestsForClass`.

Naming convention: `src/Foo.res` ↔ `test/Foo_test.res`. Variants seen in
the wild: `Foo_test.res`, `FooTest.res`, `foo_test.res`. Start with
`Foo_test.res` and keep the matcher small enough that a second convention
can be slotted in later.

See `_knowledge/jetbrains/GOTO_RELATED.md` for how we register related-file
providers elsewhere in the plugin.

This is the memory entry's "Phase 1" of the Cursive-style workflow — the
function-level focus is tracked separately as ticket `020`.

## Acceptance Criteria
1. A `ReScriptTestFinder` is registered and picked up by Ctrl+Shift+T.
2. From `src/Foo.res`, the command navigates to `test/Foo_test.res` when
   it exists.
3. From `test/Foo_test.res`, the command navigates back to `src/Foo.res`.
4. When no counterpart file exists, IntelliJ's standard "create test"
   dialog appears (no custom UI in this ticket).
5. Tests cover the happy path and the absent-counterpart fallback.

## Notes
- Do NOT try to locate the specific function under the cursor in this
  ticket — file-level only.
- Keep directory resolution simple: walk up to the project root and look
  for a sibling `test/` directory.
