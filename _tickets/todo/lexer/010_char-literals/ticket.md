---
summary: Char literals ('a') are unlexable — TICK is emitted in every state
created: 2026-07-30
---

# Char literals

## Goal
Lex ReScript char literals so `let c = 'a'` and `<Foo c='a' />` tokenize correctly.

## Context
Found during `grammar/060` cycle 10 while auditing unbraced JSX attribute values.
`bsc -only-parse` confirms `<Foo c='a' />` is valid ReScript, but `'` lexes as `TICK`
in every lexer state, so char literals are unrepresentable.

Deliberately excluded from `grammar/060`: it is a whole new token family affecting the
entire language, not a JSX concern. Pulling it into a JSX cycle would have smuggled a
cross-cutting change into a narrow ticket.

Note the ambiguity to resolve: `'` is also polymorphic-variant / type-variable syntax
(`'a` as a type parameter), so this needs real disambiguation rather than a naive rule.

## Acceptance Criteria
1. A `CHAR` token exists and lexes correctly, with fixtures.
2. Type parameters (`'a`) still lex correctly — pinned by a fixture.
3. Char literals work as JSX attribute values.
4. Restart/incremental-lexing invariants preserved.
