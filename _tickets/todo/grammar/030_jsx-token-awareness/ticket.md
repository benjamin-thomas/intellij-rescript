---
summary: Lex JSX tags (<div>, <Component />) with disambiguation against comparison
created: 2026-04-11
---

# JSX token awareness

## Goal
Teach the lexer to recognize JSX tags so `<div>`, `</div>`, `<Component />`
are emitted as structured tokens instead of a sequence of `LT`, identifiers,
`GT`.

## Context
Disambiguation challenge: `<` following an identifier is comparison
(`a < b`); `<` in an expression position is JSX. Same shape as the regex-vs-
division problem (ticket `010`), so the two should share a single "previous
token class" helper.

See `_knowledge/lexer/OVERVIEW.md` for lexer-state management and
`_knowledge/parser/OVERVIEW.md` for how tokens flow into the parser.

## Acceptance Criteria
1. Lexer enters a JSX state on `<` when the previous meaningful token is
   not an expression-end (identifier, literal, `)`, `]`).
2. Tags emit distinct tokens: `JSX_TAG_OPEN`, `JSX_TAG_NAME`,
   `JSX_ATTR_NAME`, `JSX_TAG_CLOSE`, etc.
3. Self-closing tags (`<Component />`) are handled.
4. Nested JSX inside expressions (`<div>{value}</div>`) round-trips
   through lexer snapshot tests.
5. Existing comparison-operator tests still pass.

## Notes
- Defer JSX parser rules to a follow-up ticket — this ticket is lexer-only.
- Coordinate the "previous token class" helper with ticket `010` (regex).
- Attribute values that are expressions (`onClick={handler}`) need the
  brace-tracking infrastructure from ticket `020`.
