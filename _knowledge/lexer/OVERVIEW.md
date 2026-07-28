# Lexer — How syntax highlighting works

## Overview

The lexer turns source text into a stream of **tokens**. Each token has a type
(`LET`, `LIDENT`, `INT`, `STRING`, etc.) and carries its raw text. The lexer
doesn't understand structure — it just chops text into labeled pieces.

```
Source:  let x = 42
Tokens:  LET('let')  WHITE_SPACE(' ')  LIDENT('x')  WHITE_SPACE(' ')  EQ('=')  WHITE_SPACE(' ')  INT('42')

Source:  "hello\nworld"
Tokens:  STRING_START('"')  STRING_CONTENT('hello')  STRING_ESCAPE('\n')  STRING_CONTENT('world')  STRING_END('"')
```

## Technology: JFlex

We use [JFlex](https://jflex.de/), a lexer generator for Java. You write regex
rules in a `.flex` file, JFlex generates a Java class that implements the lexer.

**Our `.flex` file:** `src/main/kotlin/.../lang/ReScript.flex`

**Generated output:** `src/main/gen/.../lang/_ReScriptLexer.java` (gitignored)

**Generation:** Automatic on every build via the `generateLexer` Gradle
task (GrammarKit plugin).

### JFlex basics

A `.flex` file has three sections separated by `%%`:

```
[header — package, imports, class declaration]
%%
[directives — class name, interface, macros]
%%
[rules — regex patterns mapped to token types]
```

### Macros

Macros define reusable character classes:

```
WHITE_SPACE = [ \t\n\r]+
LOWER_IDENT = [a-z_][a-zA-Z0-9_]*
INT = [0-9]+
```

JFlex does NOT support `\s`, `\d`, `\w` shortcuts — use explicit character
classes.

### States

The lexer starts in `YYINITIAL` (the default state) and switches to
purpose-specific states when it encounters certain tokens. Each state has its
own set of rules. The lexer exits a state when it encounters the matching
closing token (e.g., `"` exits `IN_STRING`, `` ` `` exits `IN_TEMPLATE`,
`*/` at depth 0 exits `IN_BLOCK_COMMENT`).

This mechanism is how the lexer handles constructs where the same character
means different things depending on context — a `\n` inside a string is an
escape sequence, but outside it's whitespace. States are declared with
`%state NAME` in the `.flex` file, and transitions use `yybegin(STATE)`.

The declared states (11 plus `YYINITIAL`):

- `REGEX`, `IN_STRING`, `IN_TEMPLATE`, `IN_BLOCK_COMMENT` — the classics
- `JSX_TAG` (inside `<name …`), `JSX_CHILDREN` (between `>` and `</`),
  `JSX_CLOSE_TAG` (inside `</name …`)
- context-return clones: `IN_TAG_STRING`/`IN_CHILD_STRING` and
  `IN_TAG_TEMPLATE`/`IN_CHILD_TEMPLATE` — the same string/template rules, but
  their closing quote returns to `JSX_TAG`/`JSX_CHILDREN` instead of
  `YYINITIAL`. Bodies are shared via JFlex rule groups
  (`<IN_STRING, IN_TAG_STRING, IN_CHILD_STRING> { … }`); only the exit rule
  differs per state.

`JSX_CHILDREN` shares `YYINITIAL`'s token bulk the same way
(`<YYINITIAL, JSX_CHILDREN> { … }`) and overrides only the JSX-specific and
context-entering rules. Invariant to preserve: an override must never have a
same-length competitor in the shared block, because JFlex silently prefers
whichever rule comes first in the file.

Some states need auxiliary data: `IN_BLOCK_COMMENT` tracks a `commentDepth`
counter for nesting, brace regions (`${…}`, JSX `{attr}`/`{child}`) are
tracked by a packed context-frame stack, and `REGEX` uses `yypushback(1)` to
re-consume the `/` after the regex-vs-division decision (see "Pushback
technique" below). All of it round-trips through the packed restart int —
see `_knowledge/lexer/RESTART_STATE.md`.

### Previous-token disambiguation (`/` and `<`)

`track(...)` wraps every rule's return and records whether the token is an
"expression end" (`isExpressionEnd()`: identifiers, literals, `)`, `]`, `}`,
string/template ends, and `/>` — a completed element is a value) in the
`prevIsExprEnd` boolean, which is packed into restart state (bit 7). The `>`
finishing a closing tag is also an expression end, but shares `JSX_GT` with
opening tags, so its rule sets the bit itself via `trackExprEnd(...)`:

- `/` after an expression end is division; otherwise it starts a regex.
- `<` after an expression end is comparison or a type parameter
  (`a < b`, `list<int>`); otherwise — with a one-char lookahead requiring
  `[A-Za-z_>/]` — it starts a JSX tag (`JSX_LT`), and `</` a closing tag
  (`JSX_LT_SLASH`).

**INVARIANT — expression end ⇒ operator.** After any value-producing token,
`/` and `<` are always operators, with no exceptions — completed JSX
elements included. This deliberately matches how Babel and TypeScript lex
(`<A /> / b` divides, `<A /> < B` compares), so intuitions trained on the
JS ecosystem transfer. Any future token-boundary disambiguation (e.g.
ticket 010's regex internals) must consume `isExpressionEnd()` /
`prevIsExprEnd` rather than invent a parallel rule.

**CONSTRAINT — the four delimiters are the complete JSX vocabulary.**
Never add tag-name or attribute-name token types. Names stay
`LIDENT`/`UIDENT` so identifier-based features (find usages, rename,
spellchecking) treat them uniformly; role classification — including
tag/attr coloring — belongs exclusively to the JsxElement parser rules and
annotator (ticket 060). A highlighting-only remap of in-tag identifiers was
considered and declined in favor of waiting for the parser ticket.

### Pushback technique (`yypushback`)

JFlex's `yypushback(n)` puts `n` characters back into the input so they can be
re-consumed in a different state. We use this for **regex vs division**
disambiguation:

1. In `YYINITIAL`, we see `/` and check the previous token to decide regex vs
   division.
2. If regex: call `yypushback(1)` to un-eat the `/`, switch to `REGEX` state.
   The `REGEX` state then re-matches the `/` as part of the full `/pattern/flags`
   rule.
3. If division: just return `SLASH`.

We do NOT use pushback for strings or templates — `"` and `` ` `` are
unambiguous, so we consume them directly and switch state in one step.

### Rule priority

JFlex uses two rules for disambiguation:

1. **Longest match wins** — `letter` matches `LOWER_IDENT` (7 chars), not
   `LET` (3 chars), because it's longer.
2. **First rule wins on ties** — bare `_` (1 char) could match either
   `LOWER_IDENT` or the literal `"_"` rule. We put `"_"` before
   `{LOWER_IDENT}` so it wins.

Multi-character operators must also be listed before single-character ones:
`==` before `=`, `->` before `-`, `...` before `.`.

## The Adapter pattern

JFlex generates a class implementing `FlexLexer`. IntelliJ expects a `Lexer`.
These are different interfaces for the same job — tokenizing text.

JetBrains ships `FlexAdapter` as the default bridge, but ReScript uses a
custom `ReScriptLexerAdapter` because template interpolation needs richer
incremental restart state than raw JFlex lexical state can represent.

See `_knowledge/lexer/RESTART_STATE.md` for the detailed rationale. At a high
level, the adapter exists so IntelliJ's single integer restart state can encode
all context needed to restart correctly inside `${...}`.

The old minimal form would have been:

```kotlin
class ReScriptLexerAdapter : FlexAdapter(_ReScriptLexer(null))
```

The current adapter is still the same
[Adapter pattern](https://en.wikipedia.org/wiki/Adapter_pattern) — a wrapper
that translates one API to another — but it also packs and restores restart
state for incremental lexing.

## Token types

Each token type is an instance of `IElementType` registered with our language:

```kotlin
class ReScriptTokenType(debugName: String) : IElementType(debugName, ReScriptLanguage)
```

All token constants are generated by GrammarKit in `ReScriptTypes.java` (from
the `tokens` block in `ReScript.bnf`). The same file also holds composite element
types like `LET_DECLARATION`. Both the lexer (`.flex`) and the syntax highlighter
reference these constants.

```java
// In generated ReScriptTypes.java:
IElementType LET = new ReScriptTokenType("LET");        // token (lexer returns this)
IElementType LET_DECLARATION = new ReScriptElementType("LET_DECLARATION"); // composite (parser creates this)
```

`WHITE_SPACE` and `BAD_CHARACTER` come from IntelliJ's built-in `TokenType`
class, not from our custom types.

## Syntax highlighting

`ReScriptSyntaxHighlighter` maps token types to colors:

```kotlin
override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    val key = when (tokenType) {
        ReScriptTypes.LET, ReScriptTypes.TYPE, ... -> KEYWORD
        ReScriptTypes.LIDENT -> IDENTIFIER
        ReScriptTypes.STRING_START, ReScriptTypes.STRING_END,
        ReScriptTypes.STRING_CONTENT,
        ReScriptTypes.TEMPLATE_START, ReScriptTypes.TEMPLATE_END,
        ReScriptTypes.TEMPLATE_CONTENT -> STRING
        ReScriptTypes.STRING_ESCAPE -> STRING_ESCAPE   // distinct color for \n, \t, etc.
        // ...
    }
    return arrayOf(key)
}
```

The highlighter uses `DefaultLanguageHighlighterColors` as fallbacks (e.g.,
`KEYWORD` falls back to whatever the user's theme defines for keywords).

`ReScriptSyntaxHighlighterFactory` creates the highlighter and is registered
in `plugin.xml` via `<lang.syntaxHighlighterFactory>`.

**Key property**: syntax highlighting is instant and offline. It runs the JFlex
lexer in-process — no LSP server needed, no network round-trip. This is why we
have both a lexer (for fast coloring) and an LSP (for semantic features).

## Testing strategy

### Snapshot tests (gold file comparison)

Each test has two fixture files:
- `Keywords.res` — source input
- `Keywords.out` — expected token stream

The test function `runSnapshotTest` in `LexerTestUtils.kt`:
1. Loads the `.res` file
2. Runs the lexer via `LexerTestCase.printTokens()` (IntelliJ's static method)
3. Compares the output against the `.out` file via
   `UsefulTestCase.assertSameLinesWithFile()` (IntelliJ's static method)

If the `.out` file doesn't exist, `assertSameLinesWithFile` **auto-creates it**
with the actual output and fails the test. This is snapshot testing — run once
to generate, review the output, run again to pass. Same concept as
[Jest snapshots](https://jestjs.io/docs/snapshot-testing).

### Why standalone functions instead of LexerTestCase inheritance

IntelliJ's `LexerTestCase` provides `printTokens()` as a `public static`
method, so we call it directly without inheriting from the class. This avoids
unnecessary inheritance and keeps our test class clean:

```kotlin
class ReScriptLexerTest {
    private fun checkLexer(inputFile: String, expectedOutputFile: String) =
        runSnapshotTest(ReScriptLexerAdapter(), inputFile, expectedOutputFile)

    @Test
    fun testKeywords() = checkLexer("Keywords.res", "Keywords.out")
}
```

### Incremental lexing correctness

Two additional tests verify the lexer works correctly for IntelliJ's incremental
re-lexing (when the user edits a file, IntelliJ re-lexes from a saved position
rather than from the beginning):

- **`checkZeroState`** — verifies that keywords and identifiers always leave the
  lexer in state zero (a performance property: the classic highlighter only
  restarts at state-0 tokens). It takes an `ignorableStateBits` mask for the
  `prevIsExprEnd` bit, which legitimately makes tokens after an expression end
  non-zero and self-corrects after restart. Ticket `grammar/050` (RestartableLexer)
  would retire this check entirely.

- **`checkCorrectRestart`** — lexes the full text, then restarts the lexer from
  every token boundary and verifies the remaining tokens match. Catches bugs in
  state save/restore.

Template interpolation specifically relies on this restart correctness: the
restart state must preserve more than just raw JFlex lexical state so the
closing `}` of `${...}` restarts as `TEMPLATE_INTERPOLATION_END`, not plain
`RBRACE`. See `_knowledge/lexer/RESTART_STATE.md`.

### Observing tokens in runIde

Run `./gradlew runIde`, open a `.res` file, then use
**Tools > View PSI Structure** (PSI Viewer). With our `ParserDefinition`
registered, it shows individual tokens as `PsiElement(TOKEN_TYPE)` nodes.

## Current limitations (TODOs)

- **Block comments inside JSX regions**: `/* … */` between tags or inside an
  opening tag mis-lexes (the comment states hard-exit to `YYINITIAL`;
  tag/children would need `IN_TAG_BLOCK_COMMENT`/`IN_CHILD_BLOCK_COMMENT`
  clones like the string/template ones). Line comments work.
- **`RestartableLexer`**: see ticket `grammar/050_restartable-lexer`.

## Reference implementations

- **Haskell plugin** (`tmp/intellij-haskell-lsp/`): Uses JFlex states for nested
  block comments (`NCOMMENT` with `commentDepth`), Haddock docs, quasi-quotes,
  and GHC pragmas. Good reference for stateful lexing.
- **Elm plugin** (`tmp/intellij-elm/`): Has a layout lexer (`ElmLayoutLexer`)
  that injects virtual tokens for Elm's offside rule. Not needed for ReScript
  (uses braces), but interesting architecture.
- **Rust plugin** (`tmp/intellij-rust/`): Comprehensive lexer tests including
  a fuzzy test that feeds 10,000 random strings to verify no crashes. Good
  model for test coverage.
