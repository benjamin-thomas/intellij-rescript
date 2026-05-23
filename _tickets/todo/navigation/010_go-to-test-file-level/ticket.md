---
summary: Ctrl+Shift+T jumps between src/Foo.res and tests/FooTest.res
created: 2026-04-11
---

# Go to Test — file-level navigation

## Goal
Wire up Ctrl+Shift+T so the user can jump from a source file to its test
file (or vice versa) using a single mirror convention.

## Context
IntelliJ ships `GotoTestOrCodeHandler` for exactly this purpose. The
extension point is `testFinder` (`com.intellij.testFinder`) which returns a
`TestFinder` producing `findClassesForTest` / `findTestsForClass`.

Convention (single, unambiguous): `src/Foo.res` ↔ `tests/FooTest.res`.

We deliberately support only one mapping. The reason is ticket `030`
(create-if-missing): if multiple counterpart locations were valid, the
auto-create path would have to ask the user where to put the new file,
which adds friction. A single mapping means "missing" implies a single
known destination.

If users later need flexibility, we can make the mirror path
configurable (e.g., a project-level setting for the test directory or a
custom suffix). That is out of scope for this ticket.

See `_knowledge/jetbrains/GOTO_RELATED.md` for how we register
related-file providers elsewhere in the plugin.

This is the memory entry's "Phase 1" of the Cursive-style workflow — the
function-level focus is tracked separately as ticket `020`.

## Acceptance Criteria
1. A `ReScriptTestFinder` is registered and picked up by Ctrl+Shift+T.
2. From `src/Foo.res`, the command navigates to `tests/FooTest.res` when it exists.
3. From `tests/FooTest.res`, the command navigates back to `src/Foo.res`.
4. When the counterpart file does not exist, IntelliJ's standard "create test" dialog appears (no custom UI in this ticket).
5. Tests cover the happy path (both directions) and the absent-counterpart fallback.

## Notes
- Do NOT try to locate the specific function under the cursor in this
  ticket — file-level only.
- Directory resolution: walk up to the project root and look for a
  sibling `tests/` directory.
- The matcher should stay small enough that swapping in a configurable
  mirror path later is a trivial change.
