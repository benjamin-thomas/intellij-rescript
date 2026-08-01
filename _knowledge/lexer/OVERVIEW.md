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

**`IN_BLOCK_COMMENT` deliberately has NO such clones**, though it is entered
from four states and must return to the right one. It uses a live
`blockCommentReturn` field instead, because unlike a string or template it emits
no token until its terminating `*/` — the whole nested comment is consumed
inside one `advance()`, so no token boundary can land inside it and the field is
always written before it is read. Cloning would have spent three of the four
remaining lexical-state ids to insure against that. `ReScriptLexerTest.
testBlockCommentStateIsNeverObservable` pins the property; if a future change
makes the comment state emit tokens, the return state has to move into the
packed restart int and that test says so.

`JSX_CHILDREN` shares `YYINITIAL`'s token bulk the same way
(`<YYINITIAL, JSX_CHILDREN> { … }`) and overrides only the JSX-specific and
context-entering rules. Invariant to preserve: an override must never have a
same-length competitor in the shared block, because JFlex silently prefers
whichever rule comes first in the file.

Some states need auxiliary data: `IN_BLOCK_COMMENT` tracks a `commentDepth`
counter for nesting, brace regions (`${…}`, JSX `{attr}`/`{child}`) are
tracked by an unbounded context-frame stack (packed into the restart int only
as a lossy hint — see `RESTART_STATE.md`), and `REGEX` uses `yypushback(1)` to
re-consume the `/` after the regex-vs-division decision (see "Pushback
technique" below). The comment depth and both decision bits round-trip through
the packed restart int; the frame stack only fits it up to three frames, and
past that the packed form is knowingly lossy — see
`_knowledge/lexer/RESTART_STATE.md`.

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
- **…unless a line break intervenes** (`sawLineBreak`), in which case it starts
  a tag regardless. This is what makes an element in statement position parse —
  a `@react.component let make` body returns one right after a `let` ending in
  `}`.

**The rule, exactly.** An LF occurring after the end of the last significant
token and before the `<`. Measured against `bsc -dparsetree`, not guessed:

| input | bsc |
| --- | --- |
| `a<b`, `a <b` | comparison |
| `a` ⏎ `<b` | JSX element |
| `a` CR `<b` (lone CR) | comparison — a bare CR is not a break |
| `1` ⏎ `/* c */ <div />` | JSX — a comment between break and `<` is transparent |
| `1 /* c` ⏎ `*/ <div />` | JSX — **the break may live inside the comment** |
| `a /* c */ <b` | comparison — no break anywhere |
| `"a` ⏎ `b" <div />` | comparison — a break inside the previous *token* does not count |

`sawLineBreak` is set by every rule that consumes a newline (whitespace runs in
each state, and block-comment interiors) and cleared by `track()` on each
significant token, which reproduces that table exactly.

**INVARIANT — expression end ⇒ operator, absent a line break.** After any
value-producing token, `/` and `<` are operators — completed JSX elements
included. This deliberately matches how Babel and TypeScript lex (`<A /> / b`
divides, `<A /> < B` compares), so intuitions trained on the JS ecosystem
transfer. Any future token-boundary disambiguation (e.g. ticket 010's regex
internals) must consume `isExpressionEnd()` / `prevIsExprEnd` rather than
invent a parallel rule.

**`/` does NOT get the line-break exemption.** Only `<` does. ReScript's
formatter trails binary operators (`a /` ⏎ `b`), so a leading `/` is rare, and
letting one start a regex would swallow the rest of the file.

**Unbraced attribute values are ReScript *primary* expressions** — an atomic
expression followed by call/index postfixes. That is literally what the compiler
does: its JSX parser calls `parse_primary_expr ~operand:(parse_atomic_expr p)`.
`(` and `[` in `JSX_TAG` therefore open a region (frame kind `JSX_VALUE`) whose
interior lexes as ordinary expression text, so `b=Some(-1)` and `b=f(x/2)` work
while `b=-1` stays impossible — the `-` exclusion guards where a value *starts*,
not what a region contains.

**KNOWN GAPS in that rule**, all measured against bsc:
- `b=list{1, 2}` and `b=dict{"k": v}` are legal and not supported. A braced
  suffix on a path cannot be distinguished from `<A b=x {...p} />` — also legal,
  and far commoner — without whitespace sensitivity. This one is permanent
  absent a lexer-level "no space before `{`" rule.
- `b=<C />` (a bare element as an unbraced value) is legal and not supported;
  `<` in a tag is still `BAD_CHARACTER`. A scope cut, not an impossibility — the
  JSX_VALUE frame could carry a 1-bit elem/paren flag in its payload.
- `` b=f`x` `` (tagged template) is legal and not supported.
- **Over-acceptance:** the grammar is whitespace-insensitive, so `b=f` ⏎ `(x)`
  parses here and bsc rejects it — a postfix call must stay on its line.
- An unclosed value region (`<A b=f(`) is bounded by `jsxValueContent`, which
  omits `declKeyword`, so it stops at the next `let`/`type`/`module`. The
  lexer-side `JSX_DECL_RESCUE` only covers `let`/`and`, so the lexer itself does
  not bail before a `type`/`module` — pre-existing, newly reachable.

**KNOWN DIVERGENCE — wrapped type applications.** `let xs: array` ⏎ `<int>` is
a type application to bsc and a JSX element to this lexer; the two are the same
token sequence and only parser context separates them. The whole family is
affected — `type` bodies, return-type annotations, module bodies — anywhere a
type application wraps before its `<`. Accepted: the formatter collapses the
shape, the damage stays inside the one declaration, and the alternative is
losing statement-position JSX. Pinned by the parser fixture
`JsxStatementPositionTypeClash`.

**Why a tracked bit and not a backward buffer scan.** A scan cannot see a break
inside a block comment (row 5 of the table), and scanning backwards *over* a
comment is not implementable — `"a */" <div />` shows the closer may be string
content, and telling those apart is what forward lexing is for. The bit also
keeps the signal inside the restart int, which is what stops the editor
highlighter converging early on an unchanged whitespace token (see
`RESTART_STATE.md`).

**CONSTRAINT — no JSX-specific token types beyond the four delimiters.**
`JSX_TAG` does lex `(`/`[` (they open an unbraced attribute value's region),
but as the ordinary `LPAREN`/`LBRACKET`, not JSX variants.
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
  restarts at state-0 tokens). It takes an `ignorableStateBits` mask for the two
  decision bits (`prevIsExprEnd`, `sawLineBreak`), which legitimately make
  ordinary tokens non-zero without placing the lexer inside a lexical region —
  the first token of every line carries the line-break bit. Ticket `grammar/050` (RestartableLexer)
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

- **`RestartableLexer`**: see ticket `grammar/050_restartable-lexer`.

## Reference implementations

Not vendored — clone from GitHub when one is needed.

- **Haskell plugin**: Uses JFlex states for nested block comments (`NCOMMENT`
  with `commentDepth`), Haddock docs, quasi-quotes, and GHC pragmas. Good
  reference for stateful lexing.
- **Elm plugin**: Has a layout lexer (`ElmLayoutLexer`) that injects virtual
  tokens for Elm's offside rule. Not needed for ReScript (uses braces), but
  interesting architecture.
- **Rust plugin**: Comprehensive lexer tests including a fuzzy test that feeds
  10,000 random strings to verify no crashes. Good model for test coverage.

## Verifying ReScript syntax — ask the compiler

There is a real ReScript compiler in the repo:

```
rescript-playground-example/node_modules/.bin/bsc -only-parse file.res
```

**Check the exit status, not the visible output.** `bsc` prints a leading blank line before
a syntax error, so a harness that samples the first line (or uses a `${out:-LEGAL}`
fallback) reports errors as legal. That mistake put a compiler-invalid form
(`<Foo neg=-1 />`) into a specification once before it was caught.

Inferring syntax from the manual or from reasoning has been wrong repeatedly. The manual
also disagrees with the compiler in at least one place: it says JSX prop names cannot
contain hyphens, but `bsc` accepts them.

## JSX lexer states — traps

`<JSX_TAG>` had no newline bailout while `WHITE_SPACE = [ \t\n\r]+`, so an unclosed `<div`
swallowed every following declaration as attribute soup. The rescue is a first-position rule
with an UNCONSUMED trailing context, so the keyword re-lexes properly in `YYINITIAL`:

```
[\r\n]+ [ \t]* / {JSX_DECL_RESCUE} { ...popFrame guard...; yybegin(YYINITIAL); return WHITE_SPACE; }
```

- The discriminator is the SHAPE AFTER the keyword, not the keyword. `<A b=` ⏎ `module(M) />`
  is legal — a first-class module is an unbraced value — so `module(` must not fire, while
  `module M = …` must. (Declaration keywords are NOT legal attribute names: `bsc` rejects
  `<input type="text" />`.)
- Indentation must be allowed — declarations nest inside modules and function bodies, which
  is exactly where JSX lives.
- `{WHITE_SPACE}` already matches `"\n  "`. A rule matching only `"\n"` silently never fires
  when the next line is indented.
- Never use JFlex's `^` anchor here: `yyreset` sets `zzAtBOL = true`, so an incremental
  restart mid-line would fire where a full lex would not, breaking `checkCorrectRestart`.
- Pop an abandoned `JSX_CONTENT` frame, but guard on `jsxContentBraceDepth() == 0` so a live
  `${…}` interpolation's return state survives.

Hyphenated web-component names lex as ONE token via a macro, deliberately not
`LIDENT MINUS LIDENT`. Keeping `-` unlexable in the tag states means the invalid
`<Foo neg=-1 />` cannot form by construction rather than by a grammar guard.
