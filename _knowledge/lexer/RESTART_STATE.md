---
summary: Packed lexer restart state (v2) — lexical state, prev-token bit, and a kinded context-frame stack in one int
updated: 2026-07-28
relates: [architecture, parser]
---

# Lexer restart state

## Why this exists

IntelliJ does **incremental lexing** during editing. It does not always re-lex
the file from the beginning after each change. Instead, it can restart the
lexer from a saved token boundary and pass back the lexer's saved state:

```kotlin
lexer.start(buffer, restartOffset, endOffset, savedState)
```

This makes lexer restart state a **correctness requirement**, not a performance
detail. If the saved state is incomplete, the lexer can produce a different
token stream after restart than it produced during a full left-to-right lex.

Three constructs need context beyond JFlex's raw lexical state:

- template interpolation (`` `a ${expr}` `` — the `}` must know it closes an
  interpolation, and how many braces deep it is)
- JSX (`<div>` tag/children states, `{attr}` and `{child}` brace regions)
- previous-token classification (`/` is division after a value, regex start
  otherwise; `<` is comparison after a value, JSX start otherwise)

`checkCorrectRestart(...)` in `LexerTestUtils.kt` is the gate: it restarts the
lexer at every token boundary and demands an identical token stream. It
reproduced real bugs twice: the interpolation `}` mis-lex before packing
existed, and `let a = x / y / z` re-lexing `/ y /` as a REGEX when the
previous-token class was not packed (fixed by the `prevIsExprEnd` bit).

## What the default adapter does

JetBrains' default `FlexAdapter` stores only the JFlex lexical state
(`yystate()`) as the restart integer returned by `Lexer.getState()`. That is
NOT sufficient here, which is why `ReScriptLexerAdapter` is a custom
`LexerBase` that saves/restores a richer packed state (see
"The role of the custom adapter" below).

## The packed restart-state layout (v2)

IntelliJ's lexer API gives the lexer only a single `Int`. The generated lexer
packs four fields into it:

- bits `0..4`  — JFlex lexical state (11 states declared; even ids ≤ 22)
- bits `5..6`  — block-comment nesting depth, saturating at 3
- bit  `7`     — `prevIsExprEnd`: previous significant token is an
  "expression end" (identifier, literal, closing delimiter, string/template
  end, or the end of a completed JSX element — `/>` or a closing tag's `>`).
  Drives `/` regex-vs-division and `<` JSX-vs-comparison. Public mask
  `PREV_IS_EXPR_END_MASK` lets `checkZeroState` ignore it (the bit
  self-corrects on the first significant token lexed after a restart).
- bits `8..31` — context stack: three 8-bit frames, low frame = innermost

### Context frames

A frame remembers why a `{` region was opened so the matching `}` can restore
the right lexer state. Frame byte = 2-bit kind (high) + 6-bit payload:

| Kind | Meaning | Payload | `}` at depth 0 returns to |
|---|---|---|---|
| `TEMPLATE` (0) | `${...}` interpolation | 2-bit return-state selector (bits 4..5: IN_TEMPLATE / IN_TAG_TEMPLATE / IN_CHILD_TEMPLATE) + 4-bit brace depth | the selected template state, emitting `TEMPLATE_INTERPOLATION_END` |
| `JSX_ATTR` (1) | `{...}` attr expression or spread in an opening tag | 6-bit brace depth | `JSX_TAG` |
| `JSX_CONTENT` (2) | one JSX children region | 3-bit count of consecutively nested unbraced elements (bits 3..5) + 3-bit `{child}` brace depth (bits 0..2) | `JSX_CHILDREN` while count > 0; frame pops to expression context on the outermost closing tag |

A live frame always has a non-zero payload (depth ≥ 1, or count ≥ 1), so a
`0x00` byte unambiguously means "empty slot".

`JSX_CONTENT` counts consecutive unbraced nesting (`<div><ul><li>` = one frame,
count 3) so a fresh frame is needed only per `{...}` alternation level. The
flagship shape `table > {map > tr > {map > td > {expr}}}` uses exactly the
three available frames (see `JsxNestedBracedElements` lexer fixture).

### Saturation policy

All counters saturate at their field maximum, and the in-memory counters use
the same limits, so full lexing and restarted lexing agree even past a limit:

- comment nesting: 3
- template interpolation depth: 15 per frame
- JSX attr-expression brace depth: 63
- JSX child-expression brace depth: 7; unbraced element nesting: 7 per frame
- context frames: 3 (pushing onto a full stack drops the outermost frame)

Past a limit the lexer mis-scopes a `}` or closing tag near the overflow
point — local mis-highlighting the permissive parser swallows — but it never
diverges between full lex and restart, which is the invariant that matters.

## Encoding and decoding

```java
private int packRestartState(
        int lexicalState, int commentDepth, boolean prevIsExprEnd, int contextStack) {
    int packedLexicalState = lexicalState & LEXICAL_STATE_MASK;
    int packedCommentDepth = (commentDepth & COMMENT_DEPTH_MASK) << COMMENT_DEPTH_SHIFT;
    int packedPrevIsExprEnd = prevIsExprEnd ? PREV_IS_EXPR_END_MASK : 0;
    int packedContextStack = (contextStack & CONTEXT_STACK_MASK) << CONTEXT_STACK_SHIFT;
    return packedLexicalState | packedCommentDepth | packedPrevIsExprEnd | packedContextStack;
}
```

Decoding shifts right and masks (`unpackLexicalState`, `unpackCommentDepth`,
`unpackPrevIsExprEnd`, `unpackContextStack`).

### Bit layout example

Lexing `let x = <div> {a` — inside a child brace region of one element, after
an identifier:

```text
lexical state (YYINITIAL = 0)     00000000 00000000 00000000 00000000
prevIsExprEnd (after `a`)         00000000 00000000 00000000 10000000
context stack << 8:
  JSX_CONTENT, count 1, depth 1   00000000 00000000 10001001 00000000
------------------------------------------------------------------
packed restart state              00000000 00000000 10001001 10000000
```

(The frame byte `10 001 001` reads: kind 2 = JSX_CONTENT, count 1, depth 1.)

### Stack behavior

Entering a nested context pushes the existing frames one byte left (the
outermost frame falls off when all three slots are full); closing the
innermost context pops one byte right. Nested template interpolation is the
canonical example:

```rescript
let nested = `outer ${`inner ${value}`}`
```

When the lexer sees the inner `${`, it must remember the outer interpolation
is still active; otherwise the outer `}` becomes a plain `RBRACE`.

For an interactive version of this model, run:

```bash
ruby _knowledge/lexer/scripts/packed_stack_demo.rb
```

## Who reads and writes the state

The changing values behind the constants:

- `zzLexicalState` — JFlex's current lexical state, via `yybegin(...)`
- `commentDepth` — block-comment rules (saturating)
- `prevIsExprEnd` — `track(...)` sets it from `isExpressionEnd(type)` on every
  significant token; the closing-tag `>` rule forces it true via
  `trackExprEnd(...)` (JSX_GT alone can't distinguish opening from closing)
- `contextStack` — frame push/pop/increment/decrement helpers, called from the
  `${`, `{`, `}`, `>`, `/>`, and closing-tag rules

### Save path for restart

IntelliJ asks the adapter for `getState()`; the adapter returns
`flex.packedRestartState` (Kotlin property syntax for the generated
`getPackedRestartState()`), captured at each token start.

### Restore path on incremental re-lex

`ReScriptLexerAdapter.start(...)` passes the saved integer into the generated
lexer's `resetWithPackedRestartState(...)`, which restores `prevIsExprEnd`,
`commentDepth`, and `contextStack`, then calls the generated `reset(...)` to
re-enter the lexical state.

## The role of the custom adapter

`ReScriptLexerAdapter` exists because the default `FlexAdapter` only persists
raw JFlex lexical state. The custom adapter wraps `_ReScriptLexer` directly:
save packed state on `getState()`, restore on `start(...)`. This keeps
restart correctness without changing IntelliJ's single-`Int` lexer API.

A future alternative is the platform's `RestartableLexer` interface, which
would let ANY packed state be a restart point and retire `checkZeroState`
entirely — see ticket `grammar/050_restartable-lexer`.

## Relation to parser completeness

This mechanism is independent of full expression parsing. The parser is still
permissive (JSX is swallowed as token soup via `nonDeclToken`), but restart
correctness is a lexer-level invariant: even with an intentionally incomplete
parser, the lexer must restart to the same token stream it would produce
during a full lex.
