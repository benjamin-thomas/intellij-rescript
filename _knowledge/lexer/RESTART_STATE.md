---
summary: Template interpolation needs packed lexer restart state beyond raw JFlex lexical state
updated: 2026-04-11
relates: [architecture, parser]
---

# Lexer restart state for template interpolation

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

That exact bug occurs for template interpolation if restart state only stores
JFlex's raw lexical state.

## What the default adapter does

JetBrains' default `FlexAdapter` stores only the JFlex lexical state
(`yystate()`) as the restart integer returned by `Lexer.getState()`.

That is sufficient when the lexer's future behavior depends only on which JFlex
state it is currently in (`YYINITIAL`, `IN_STRING`, `IN_TEMPLATE`, etc.).

It is NOT sufficient when the lexer also depends on auxiliary counters such as
nesting depth.

## Why template interpolation breaks raw lexical-state restart

Backtick templates use `IN_TEMPLATE` until the lexer sees `${`. At that point
the lexer:

1. emits `TEMPLATE_INTERPOLATION_START`
2. switches back to `YYINITIAL`
3. lexes normal ReScript tokens inside the interpolation body

That means the raw lexical state inside `${ ... }` is just `YYINITIAL`.

But `YYINITIAL` is ambiguous:

- ordinary top-level code
- code inside a template interpolation

To disambiguate the closing `}` token, the lexer also tracks
`templateInterpolationDepth`.

Without that extra depth, restart near the end of:

```rescript
`hello ${name}`
```

can mis-tokenize the final `}` as `RBRACE` instead of
`TEMPLATE_INTERPOLATION_END`.

This is not theoretical. `checkCorrectRestart(...)` reproduced exactly that
failure before the packed restart-state fix.

## Why a dedicated interpolation lexical state is not enough

A natural alternative is to introduce a dedicated JFlex state for interpolation
instead of reusing `YYINITIAL`.

That could distinguish:

- ordinary `YYINITIAL`
- interpolation code

But it still would not solve nested braces:

```rescript
`value: ${ {x: 1} }`
```

Near the inner or outer `}`, the lexer must know not just "I am in
interpolation", but also **how many braces deep** it is. A dedicated
interpolation state still needs an auxiliary depth counter.

So once nested braces are in scope, restart state must preserve more than raw
lexical state either way.

## The packed restart-state design

IntelliJ's lexer API gives the lexer only a single `Int` restart state. The
custom `ReScriptLexerAdapter` therefore packs three pieces of restart context
into one integer:

- bits `0..7`   — JFlex lexical state
- bits `8..15`  — block-comment nesting depth
- bits `16..23` — template-interpolation brace depth

Each field uses 8 bits, so each stored value ranges from `0` to `255`.

This is a pragmatic limit:
- up to 255 nested block comments
- up to 255 nested braces while lexing inside `${...}`

That limit is considered acceptable in practice.

## Encoding and decoding

The lexer packs the restart state with left shifts and bitwise OR:

```java
private int packRestartState(int lexicalState, int commentDepth, int interpolationDepth) {
    int packedLexicalState = lexicalState;
    int packedCommentDepth = commentDepth << 8;
    int packedInterpolationDepth = interpolationDepth << 16;
    return packedLexicalState | packedCommentDepth | packedInterpolationDepth;
}
```

### Bit layout example

Suppose:

- lexical state = `2`
- comment depth = `3`
- interpolation depth = `1`

Then the packed value is built like this:

```text
lexical state                 00000000 00000000 00000000 00000010
comment depth << 8            00000000 00000000 00000011 00000000
interpolation depth << 16     00000000 00000001 00000000 00000000
----------------------------------------------------------------
packed restart state          00000000 00000001 00000011 00000010
```

Decoding reverses that process:

- shift right until the desired field is in the low 8 bits
- mask with `0xFF` (`11111111`) to discard the rest

```java
private int unpackLexicalState(int packedState) {
    return packedState & 0xFF;
}

private int unpackCommentDepth(int packedState) {
    return (packedState >> 8) & 0xFF;
}

private int unpackInterpolationDepth(int packedState) {
    return (packedState >> 16) & 0xFF;
}
```

## Who reads and writes the state

The masks and shifts are constants; they are never "written". What changes over
time are the underlying values:

- `zzLexicalState` — JFlex's generated current lexical state
- `commentDepth` — maintained by block-comment rules
- `templateInterpolationDepth` — maintained by template interpolation rules

### Write path during normal lexing

- JFlex updates `zzLexicalState` via `yybegin(...)`
- comment rules increment/decrement `commentDepth`
- interpolation rules increment/decrement `templateInterpolationDepth`

### Save path for restart

IntelliJ asks the adapter for `getState()`. The adapter asks the generated
lexer for its packed restart state and stores that integer as the token's saved
restart state.

Kotlin property syntax can hide this call: `flex.packedRestartState` invokes the
generated Java getter `getPackedRestartState()`.

### Restore path on incremental re-lex

When IntelliJ restarts lexing, it calls:

```kotlin
lexer.start(buffer, restartOffset, endOffset, savedState)
```

`ReScriptLexerAdapter.start(...)` passes that saved integer into the generated
lexer's `resetWithPackedRestartState(...)`, which:

1. unpacks comment depth
2. unpacks interpolation depth
3. restores the raw JFlex lexical state
4. calls the generated `reset(...)`, which re-enters the lexical state with
   `yybegin(...)`

## The role of the custom adapter

`ReScriptLexerAdapter` exists because the default `FlexAdapter` only persists
raw JFlex lexical state.

The custom adapter wraps the generated `_ReScriptLexer` directly and exposes a
richer restart-state contract to IntelliJ:

- save packed restart state on `getState()`
- restore packed restart state on `start(...)`

This keeps interpolation restart-correct without changing IntelliJ's
single-`Int` lexer API.

## Relation to parser completeness

This mechanism is independent of full expression parsing. The parser is still
permissive in many places, but incremental lexing correctness for template
interpolation is a lexer-level invariant. Even with an intentionally incomplete
parser, the lexer must restart to the same token stream it would produce during
a full lex.
