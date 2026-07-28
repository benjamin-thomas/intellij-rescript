---
summary: Implement RestartableLexer so any packed state is a restart point
created: 2026-07-28
---

# Restartable lexer

## Goal
Implement IntelliJ's `com.intellij.lexer.RestartableLexer` interface on
`ReScriptLexerAdapter` so the editor highlighter can restart incremental
lexing at ANY token boundary, not just tokens whose state is exactly 0.
This makes the zero-state property (and its test machinery) obsolete.

## Context
- The classic `LexerEditorHighlighter` restart strategy only restarts at
  tokens with state 0, scanning backward until it finds one. Our packed
  restart int (see `_knowledge/lexer/RESTART_STATE.md`) carries COMPLETE
  lexer context by design, so every state is a sound restart point —
  `checkCorrectRestart` in `LexerTestUtils.kt` proves exactly that at
  every token boundary.
- Ticket `grammar/030` (JSX token awareness) added a `prevIsExprEnd` bit
  (bit 7) to the packed state. Tokens following an expression-end token
  (identifier, literal, closing delimiter) now have non-zero state, so
  they stopped being classic restart boundaries. Correct, but it forced
  an `ignorableStateBits` mask parameter onto `checkZeroState` — a test
  asserting a performance property that `RestartableLexer` would make
  irrelevant.
- Adapter: `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptLexerAdapter.kt`
  (custom `LexerBase`, state = `flex.packedRestartState` captured at token
  start).

## Acceptance Criteria
1. `ReScriptLexerAdapter` implements `RestartableLexer`:
   `getStartState()` returns 0, `isRestartableState()` returns true for
   every packed state (justify in a comment: the packed int is complete
   context, proven by `checkCorrectRestart`).
2. `checkZeroState` and its `ignorableStateBits` mask are deleted along
   with `testZeroStateForKeywordsAndIdentifiers`; incremental-lexing
   safety remains covered by the `checkCorrectRestart` tests.
3. All existing lexer/parser tests pass unchanged.
4. Manual smoke via `runIde`: edit mid-file inside a template literal,
   a nested JSX element, and a regex literal; highlighting stays correct
   while typing (no flicker to wrong token colors, no full-file rescan
   regressions).
5. `_knowledge/lexer/RESTART_STATE.md` updated: document the
   `RestartableLexer` contract and drop the "tokens should end in state
   zero" guidance.

## Notes
- Do this AFTER ticket `030` lands — the JSX states multiply the number
  of non-zero-state positions, which is what makes this worthwhile.
- Verify the platform actually consults `RestartableLexer` on our
  registration path (syntax highlighter lexer vs. ParserDefinition
  lexer) before relying on it; if only the classic path is used in the
  target platform version, note it and close as won't-do.
