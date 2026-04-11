---
summary: Go to Test lands on the specific test function for the symbol under the cursor
created: 2026-04-11
---

# Go to Test — function-level focus

## Goal
Extend the Go-to-Test handler so it doesn't just open the test file — it
navigates to the test function matching the implementation function under
the cursor (Cursive-for-Clojure style).

## Context
Depends on ticket `010` (file-level navigation). Once the counterpart file
is located, the handler resolves the function name under the caret and
searches the target file for a matching test.

Naming: `let foo = …` in `src/Foo.res` maps to `let test_foo = …` (or
`test("foo", …)`) in `test/Foo_test.res`. Start with the `test_<name>`
form and make the matcher pluggable for future conventions.

Requires `LetBinding` to expose the name at the caret — already available
via `PsiNameIdentifierOwner` (see `_knowledge/parser/PSI_CUSTOMIZATION.md`).

## Acceptance Criteria
1. When the caret is on `let foo`, Ctrl+Shift+T lands on `let test_foo` in
   the counterpart file.
2. When the caret is not on a top-level let, fall back to file-level
   navigation (ticket `010` behavior).
3. When the counterpart file exists but the specific test function does
   not, land at the top of the file and surface a hint ("no test for foo
   yet") — defer auto-creation to ticket `030`.
4. Tests cover: exact match, no-function-under-cursor, and missing test
   function.

## Notes
- Finding the function name at the caret is cheap — walk up PSI to the
  nearest `LetBinding` and read its name identifier.
- Do not descend into nested modules in this ticket.
