---
summary: Extract a selected expression into a let binding (extract variable) or function
created: 2026-04-11
---

# Extract variable / function

## Goal
Add IntelliJ refactorings for "Extract Variable" and "Extract Function" on
ReScript expressions: select an expression, press Ctrl+Alt+V / Ctrl+Alt+M,
and the plugin replaces the selection with a fresh `let name = <expr>` (or
function call) and inserts the binding above.

## Context
**Depends on `grammar/040_expression-parsing`.** The refactoring needs to
validate that the selected text is a complete expression, which requires
the tightened `Expression` rules — the current `opaqueBody` can't
distinguish "whole expression" from "arbitrary substring".

IntelliJ exposes extract refactorings via
`RefactoringActionHandler` + `RefactoringSupportProvider`. Reference
implementations in intellij-elm and intellij-rust handle the tricky cases
we'll also hit: selection straddling operator precedence, free variables,
and choosing where the binding is inserted.

## Acceptance Criteria
1. Extract Variable: selecting a complete `Expression` and invoking the
   refactoring replaces the selection with a fresh identifier and inserts
   `let name = <expr>` at the nearest enclosing block or top level.
2. Extract Function: same, but wraps `<expr>` in `let name = () => <expr>`
   and replaces the selection with `name()`.
3. A naming dialog lets the user pick the identifier; a default is
   suggested from the expression shape.
4. Selections that are NOT a complete expression are rejected with a
   user-facing notification (not a silent failure).
5. Undo reverts the entire refactoring atomically.
6. Tests cover: simple literal, nested call, selection-not-an-expression
   rejection, and free-variable capture.

## Notes
- Free variables: Extract Function must pass any locally-bound names used
  inside the expression as parameters. If that's too large for this
  ticket, scope v1 to closed expressions (no free variables) and add a
  follow-up for parameterized extraction.
- Insertion point: the nearest enclosing `{ … }` block; fall back to the
  enclosing top-level declaration.
