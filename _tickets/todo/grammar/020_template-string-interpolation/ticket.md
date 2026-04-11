---
summary: Lex ${expr} inside backtick strings with brace-depth tracking
created: 2026-04-11
---

# Template string interpolation

## Goal
Parse `${expr}` interpolations inside backtick strings so the embedded
expression is lexed as real ReScript tokens instead of raw string content.

## Context
Template strings are currently handled by a dedicated lexer state that
treats everything between backticks as `STRING_CONTENT`. See
`_knowledge/lexer/STRING_HANDLING.md` for the current state machine and
composite-PSI wrapping (`TemplateLiteral`).

Approach: when the lexer sees `${` inside the template state, push onto a
brace-depth stack and switch back to `YYINITIAL`. Increment on `{`,
decrement on `}`; when the counter returns to zero, pop back into the
template state.

Language injection for template strings is currently enabled via
`TemplateLiteralManipulator`. For strings that contain `${...}`, injection
must be disabled so the host language's lexer is authoritative inside the
expression.

## Acceptance Criteria
1. Lexer emits real tokens for the expression inside `${ … }`
   (identifiers, operators, literals) rather than `STRING_CONTENT`.
2. Nested braces inside the expression (`${ {x: 1} }`) don't terminate the
   interpolation early.
3. Language injection is disabled for template strings containing
   interpolations (check via a language-injection integration test).
4. Existing template-string snapshot tests continue to pass; new snapshots
   cover simple, nested, and string-literal-inside-interpolation cases.
5. PSI tree for the template literal groups interpolations as distinct
   child nodes (not flat `STRING_CONTENT`).

## Notes
- The brace-depth stack lives in the JFlex lexer as an `int` (or small
  `IntStack` if we need to support nesting inside multiple templates via
  incremental re-lex).
- Watch incremental lexing: `checkZeroState` must still restart cleanly
  mid-interpolation.
