---
summary: Highlight sub-tokens inside regex literals (pattern body vs. flags)
status: todo
form: sketch
created: 2026-04-11
---

# Regex internals highlighting

## Goal
Break regex literals (`/pattern/flags`) into sub-tokens so the pattern body
and flag letters can be colored independently in the syntax highlighter.

## Context
Regex literals are currently lexed as a single token. The lexer
(`ReScript.flex`, under `src/main/kotlin/.../lang/`) and `ReScriptSyntaxHighlighter`
are the two places to touch. The `%state` block in the flex file lists the lexer
states; the `IN_STRING` / `IN_TEMPLATE` rules show how a multi-token literal is
split.

Disambiguation: `/` is divison when it follows an expression token
(identifier, `)`, number, …) and regex-start otherwise — same pattern used
for JSX-vs-comparison, so coordinate with that work.

## Acceptance Criteria
1. Lexer emits distinct tokens for regex delimiters, pattern body, and flag
   letters.
2. `ReScriptSyntaxHighlighter` maps each new token to a color key
   (`REGEXP_PATTERN`, `REGEXP_FLAGS`, …).
3. Division vs. regex disambiguation still passes existing lexer snapshot
   tests.
4. A new lexer snapshot covers `/foo/gi`, `let x = a / b`, and
   `/[a-z]+/` inside an expression.

## Notes
- Keep the disambiguation logic small — it will share state with the future
  JSX token-awareness work (ticket `030`).
- Do not touch the parser; regex is opaque to GrammarKit.
