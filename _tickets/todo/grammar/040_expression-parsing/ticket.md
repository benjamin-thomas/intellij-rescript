---
summary: Tighten opaqueBody into real expression rules with operator precedence
created: 2026-04-11
---

# Expression parsing

## Goal
Replace the permissive `opaqueBody+` / `opaqueBody*` placeholders with a
real `Expression` rule hierarchy driven by operator precedence, so PSI
consumers can inspect expression structure.

## Context
See `_knowledge/architecture/DESIGN_NOTES.md` ("Permissive opaqueBody
strategy") for the current state and `_knowledge/parser/OVERVIEW.md`
("Phase 2: Expression hierarchy") for the planned approach.

Use `extends(".*Expr")=Expression` and list all `*Expr` rules directly in
the `Expression` alternation — GrammarKit's priority-climbing left-recursion
handling depends on that flat listing.

Precedence ladder from the tree-sitter-rescript grammar (lowest → highest):
mutation < ternary < `||` < `&&` < comparison < `+`/`-` < `**` < `*`/`/` <
pipe < await < call < member < unary.

This is a large, high-leverage change. Scope it tightly: **literals,
identifiers, binary operators, function calls, member access, parenthesized
expressions, if/else**. Defer JSX expressions, async/await sugar, pattern
matching expressions, and record/object literals to follow-up tickets.

## Acceptance Criteria
1. `Expression` rule and its concrete `*Expr` variants exist in
   `ReScript.bnf`; `opaqueBody` is no longer used inside `LetBinding`.
2. Operator precedence matches the tree-sitter-rescript spec for the
   subset listed above.
3. A new `parser/complete/` snapshot suite covers binary ops, calls,
   member access, parens, and if/else.
4. A new `parser/partial/` snapshot suite covers truncated expressions
   (`let x = 1 +`, `let y = foo(`) — each produces a `LetBinding` node
   with error markers, not an empty file.
5. All existing parser snapshot tests continue to pass (gold files
   updated where tightening legitimately changed the tree shape).

## Notes
- JSX, async, pattern-match, and record/object literals explicitly out of
  scope — leave placeholders that defer to a follow-up opaque region.
- Ticket `refactoring/010` (extract variable) depends on this.
- Expect gold-file churn — review carefully before committing.
