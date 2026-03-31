---
summary: SimpleTokenSetQuoteHandler — position-based opening/closing, hasNonClosedLiteral override
updated: 2026-03-31
relates: [lexer]
---

# SimpleTokenSetQuoteHandler

Handles auto-closing quotes (type `"`, get `""` with cursor between) and
step-over (type `"` at a closing quote, cursor moves past it).

## The non-obvious part

It does NOT distinguish opening tokens from closing tokens. All tokens passed
to the constructor are stored in a flat `TokenSet`. The handler decides opening
vs closing purely by **cursor position within the token**:

- `isOpeningQuote`: is the token in the set AND `offset == iterator.start`?
- `isClosingQuote`: is the token in the set AND `offset == iterator.end - 1`?
- `isInsideLiteral`: is the token in the set? (any position)

`STRING_START` works as an "opener" not because of its name, but because it's a
single-character token and the cursor is always at position 0 (the start) after
typing `"`.

## hasNonClosedLiteral override

The default implementation scans line tokens to detect unclosed strings, but it
assumes single-token strings. With our multi-token design (`STRING_START` +
`STRING_CONTENT` + `STRING_END`), the default gets confused and auto-pairs
inside unclosed strings. We override it to check: is the cursor on a
`STRING_START`/`TEMPLATE_START`? If yes, auto-pair (it's a fresh quote). If no,
don't (we're inside an existing string).
