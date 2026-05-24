---
summary: Design rationale for Elm-style lexer states for strings and templates
updated: 2026-05-24
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
2. **Template interpolation** — backtick templates exit back to `YYINITIAL`
   inside `${expr}`, with brace-depth tracking to know which `}` closes the
   interpolation
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
- Backtick templates support `${expr}` interpolation (the lexer re-enters
  `YYINITIAL` with brace depth tracking) — this is fundamentally different from
  double-quoted string lexing
- They may get different visual styling (e.g. a background tint on template
  strings to signal "interpolation can happen here")
- ReScript's `\n` inside backticks is a literal backslash + n, not an escape
  sequence — no `TEMPLATE_ESCAPE` needed

## Unclosed string recovery

- **Double-quoted strings**: physical newlines are valid string content and stay
  in `IN_STRING`. The lexer exits only on the closing `"`, or at EOF for an
  unclosed string.
- **Backtick templates**: no special recovery yet — the lexer stays in
  `IN_TEMPLATE` until it hits a closing backtick or EOF.

## Parser-level grouping: composite PSI nodes

At the parser level, the lexer's multi-token sequences are wrapped into composite
PSI nodes: `StringLiteral` groups `STRING_START (STRING_CONTENT | STRING_ESCAPE)*
STRING_END`, and `TemplateLiteral` groups `TEMPLATE_START` plus alternating raw
template content and `TemplateInterpolation` children before `TEMPLATE_END`.
This is required for language injection (`PsiLanguageInjectionHost` needs a
single parent node) and for PSI consumers that need interpolation holes as
distinct nodes. See `_knowledge/jetbrains/LANGUAGE_INJECTION.md`.

## Nested template literals inside interpolations

Nested backtick templates inside an interpolation are valid ReScript syntax
(verified against `bsc` 12.2.0), e.g.:

```rescript
let nested = `outer ${`inner ${x}`}`
```

The lexer handles this with a packed interpolation stack. Each active template
interpolation has its own brace-depth frame. When the lexer enters an inner
`${...}` from inside a nested template, it pushes the previous interpolation
depth and starts a fresh depth for the inner interpolation. When the inner
interpolation closes, it pops back to the outer interpolation depth.

This matters because the lexer must not let an inner template overwrite the
state needed to recognize the outer `}` as `TEMPLATE_INTERPOLATION_END`. The
stack is packed into IntelliJ's single integer restart state; see
`_knowledge/lexer/RESTART_STATE.md`.

## Regex vs division disambiguation

The `/` character is ambiguous: it could start a regex literal (`/pattern/`) or
be division (`a / b`). We check the previous significant token — if it's an
expression-end token (identifier, literal, closing delimiter), `/` is division.
Otherwise it starts a regex.

This check happens in `YYINITIAL`. If regex, we use `yypushback(1)` to put the
`/` back and switch to the `REGEX` state, which then re-consumes it as part of
the full `/pattern/flags` match. Strings and templates don't need pushback
because `"` and `` ` `` are unambiguous delimiters.
