---
summary: Offer to create a missing test function with an empty body
created: 2026-04-11
---

# Go to Test — create-if-missing

## Goal
When the user invokes Go-to-Test on a function that has no corresponding
test, offer to create one with an empty body in the test file.

## Context
Depends on tickets `010` (file-level) and `020` (function-level). This
ticket adds the final UX step: when the function lookup misses, pop a
confirmation and, on accept, insert `let test_<name> = () => { }` at the
end of the test file (or in a sensible location if we can infer one).

If the test file itself doesn't exist, use IntelliJ's standard "create
test" dialog to scaffold it first, then insert the stub.

## Acceptance Criteria
1. Missing-test-function case surfaces a non-blocking prompt ("Create
   test for `foo`?").
2. On accept, a new `let test_foo = () => { }` is appended to the test
   file and the caret navigates to the empty body.
3. Undo reverts the insertion cleanly (wraps the edit in a
   `WriteCommandAction`).
4. When the test file itself is missing, the standard create-test dialog
   opens first; on completion, the stub is inserted into the new file.
5. Cancelling the prompt leaves the source unchanged and falls back to
   ticket `020`'s "no test yet" hint.

## Notes
- Keep the generated signature minimal — no imports, no assertions, no
  test-framework-specific boilerplate. The user fills that in.
- Formatting: after insertion, let the existing formatter run so the new
  code matches project style.
