---
summary: Highlight sub-tokens inside regex literals (pattern body vs. flags)
created: 2026-04-11
---

# Regex internals highlighting

## Goal
Break regex literals (`/pattern/flags`) into sub-tokens so the pattern body
and flag letters can be colored independently in the syntax highlighter.

## Context
Regex literals are currently lexed as a single token. The lexer
(`src/main/grammars/ReScript.flex`) and `ReScriptSyntaxHighlighter` are the
two places to touch. See `_knowledge/lexer/OVERVIEW.md` for how lexer states
are structured, and `_knowledge/lexer/STRING_HANDLING.md` for the pattern
used to split a multi-token literal.

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
