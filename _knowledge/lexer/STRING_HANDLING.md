---
summary: Design rationale for Elm-style lexer states for strings and templates
updated: 2026-03-31
relates: [architecture]
---

# String and Template Handling

## Design decision: Elm-style lexer states

We chose **Elm-style lexer states** (separate tokens for opening quote, content,
escape sequences, and closing quote) over the simpler **Rust-style** approach
(optional closing quote in a single regex).

### Why not Rust-style?

Rust's lexer uses a single regex with an optional closing quote:

```
STRING_LITERAL = \" ( [^\\\"] | \\[^] )* ( \" {SUFFIX}? | \\ )?
```

A lone `"` still matches as `STRING_LITERAL`. This is simple but produces one
opaque token for the entire string — you can't highlight escape sequences
differently from normal content.

### Why Elm-style?

With lexer states, the string is broken into multiple tokens:

```
"hello\nworld"  →  STRING_START(")  STRING_CONTENT(hello)  STRING_ESCAPE(\n)  STRING_CONTENT(world)  STRING_END(")
```

This enables:
1. **Escape sequence highlighting** — `\n`, `\t`, `\"` get a distinct color
   (`DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE`)
2. **Future interpolation** — backtick templates will need the lexer to exit
   back to `YYINITIAL` inside `${expr}`, which requires state-based lexing
3. **Clean QuoteHandler** — `STRING_START` and `TEMPLATE_START` are separate
   tokens, so `SimpleTokenSetQuoteHandler` can recognize them by cursor position

## Two token families

Double-quoted strings and backtick templates have **separate token types**:

| Double quote `"` | Backtick `` ` `` |
|---|---|
| `STRING_START` | `TEMPLATE_START` |
| `STRING_CONTENT` | `TEMPLATE_CONTENT` |
| `STRING_ESCAPE` | *(not applicable)* |
| `STRING_END` | `TEMPLATE_END` |

They are separate because:
- Backtick templates will need `${expr}` interpolation (the lexer must re-enter
  `YYINITIAL` with brace depth tracking) — this is fundamentally different from
  double-quoted string lexing
- They may get different visual styling (e.g. a background tint on template
  strings to signal "interpolation can happen here")
- ReScript's `\n` inside backticks is a literal backslash + n, not an escape
  sequence — no `TEMPLATE_ESCAPE` needed

## Unclosed string recovery

- **Double-quoted strings**: a newline exits `IN_STRING` back to `YYINITIAL`.
  The unclosed string produces `STRING_START` + `STRING_CONTENT` with no
  `STRING_END`. This limits damage to one line.
- **Backtick templates**: no special recovery yet — the lexer stays in
  `IN_TEMPLATE` until it hits a closing backtick or EOF.

## Regex vs division disambiguation

The `/` character is ambiguous: it could start a regex literal (`/pattern/`) or
be division (`a / b`). We check the previous significant token — if it's an
expression-end token (identifier, literal, closing delimiter), `/` is division.
Otherwise it starts a regex.

This check happens in `YYINITIAL`. If regex, we use `yypushback(1)` to put the
`/` back and switch to the `REGEX` state, which then re-consumes it as part of
the full `/pattern/flags` match. Strings and templates don't need pushback
because `"` and `` ` `` are unambiguous delimiters.
