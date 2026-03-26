# Parser — How the PSI tree is built

## Overview

The parser takes the token stream from the lexer and builds a **PSI tree**
(Program Structure Interface) — IntelliJ's typed AST. The PSI tree is what
powers structure view, code folding, go-to-symbol, breadcrumbs, inspections,
and refactoring.

```
Flat tokens:    LET  LIDENT('x')  EQ  INT('1')

PSI tree:       ReScriptFile
                  LetDeclaration
                    PsiElement(LET)('let')
                    PsiElement(LIDENT)('x')
                    PsiElement(EQ)('=')
                    PsiElement(INT)('1')
```

## Technology: GrammarKit

[GrammarKit](https://github.com/JetBrains/Grammar-Kit) is JetBrains' parser
generator. You write a BNF-like grammar in a `.bnf` file, it generates:

1. **A parser** (Java class) — recursive descent parser
2. **A types holder** (Java class) — constants for every element type
3. **PSI classes** (optional) — interfaces and implementations for every rule

The `.bnf` file lives at `src/main/grammars/ReScript.bnf` (to be created).
Generated output goes to `src/main/gen/`.

### Relationship to the lexer

```
Source text
    |
    v
[JFlex Lexer]  (.flex)  -->  Token stream
    |
    v
[GrammarKit Parser]  (.bnf)  -->  PSI tree
```

The lexer produces tokens. The parser consumes tokens and groups them into
tree nodes. The parser references token types from the lexer (e.g., `LET`,
`LIDENT`, `EQ`).

## BNF file structure

A `.bnf` file has two parts:

### Header block

Configuration in `{ ... }` at the top:

```bnf
{
    parserClass="...ReScriptParser"
    elementTypeHolderClass="...ReScriptTypes"
    elementTypeClass="...ReScriptElementType"
    tokenTypeClass="...ReScriptTokenType"
    psiClassPrefix="ReScript"
    psiImplClassSuffix="Impl"
    // ...
}
```

### Grammar rules

```bnf
File ::= item*

private item ::= declaration
    { recoverWhile=item_recover }

private item_recover ::= !(LET | TYPE | MODULE | <<eof>>)

LetDeclaration ::= LET LIDENT EQ Expression
    { pin=1 }
```

## Error recovery

Error recovery is critical for IDE plugins — users are constantly mid-typing,
so the parser must handle incomplete/invalid code gracefully.

### `pin` — commit to a rule

`pin=N` means: once the parser matches the Nth element, **commit** to this
rule. Don't backtrack even if later elements fail. Instead, produce error
markers for the missing parts.

```bnf
LetDeclaration ::= LET LIDENT EQ Expression    { pin=1 }
```

With this, typing `let ` (incomplete) produces:

```
LetDeclaration        <-- node created despite error
  PsiElement(LET)
  PsiErrorElement     <-- "identifier expected"
```

Without `pin`, the parser would backtrack entirely and produce no node — losing
the structural information that this is a let declaration.

**When to pin**: on the keyword or token that unambiguously identifies the rule.
For `LetDeclaration`, pin on `LET` (position 1). For `TypeDeclaration`, pin on
`TYPE`. For a function call, maybe pin on `(`.

**`extendedPin`** (on by default): when pinned, the parser tries to match each
remaining element even if intermediate ones fail. So `let = 1` (missing
identifier) still produces a `LetDeclaration` with an error on the missing
`LIDENT`, but successfully parses `EQ` and `Expression`.

### `recoverWhile` — skip garbage tokens

After a rule finishes (success or failure), `recoverWhile` specifies which
tokens to skip before attempting the next rule. It's always a negative
lookahead:

```bnf
private item_recover ::= !(LET | TYPE | MODULE | OPEN | EXTERNAL | <<eof>>)
```

This means: "keep skipping tokens while we do NOT see a keyword that starts
a new declaration (or end of file)."

### The canonical pattern: `pin` + `recoverWhile`

```bnf
File ::= item*

// Dispatcher — private (no PSI node), carries recovery
private item ::= declaration
    { recoverWhile=item_recover }

// Stop at any top-level keyword or EOF
private item_recover ::= !(LET | TYPE | MODULE | OPEN | EXTERNAL | <<eof>>)

// Concrete rules — pin on their keyword
LetDeclaration ::= LET LIDENT EQ Expression    { pin=1 }
TypeDeclaration ::= TYPE LIDENT ...             { pin=1 }
ModuleDeclaration ::= MODULE UIDENT ...         { pin=1 }
```

**How it works**: The user types `let x = ` and stops.

1. Parser enters `item`, tries `LetDeclaration`.
2. Matches `LET` (pins), matches `x`, matches `=` — good so far.
3. Tries to match `Expression` — fails (nothing there).
4. Because of pin, a `LetDeclaration` node IS created with an error marker.
5. `recoverWhile` fires, skips nothing (at EOF), returns.
6. The `File ::= item*` loop exits.
7. Result: a valid PSI tree with one `LetDeclaration` containing an error.

## PSI class generation

### Option A: Generate everything (Rust plugin approach)

The default — GrammarKit generates interfaces, implementations, and a factory
for every public rule. You customize via `mixin` and `implements`:

```bnf
LetDeclaration ::= LET LIDENT EQ Expression {
    pin=1
    mixin="...LetDeclarationMixin"
    implements="com.intellij.psi.PsiNamedElement"
}
```

**Pros**: less code to maintain.
**Cons**: explosion of generated interfaces, less control.

The Rust plugin uses this approach — ~365 rules, all generating PSI classes,
with `mixin` and `stubClass` on many of them.

### Option B: Hand-written PSI (`generate = [ psi = "no" ]`) (Elm plugin approach)

GrammarKit only generates the parser and types holder. PSI classes are written
by hand.

**Pros**: full control over PSI hierarchy, can add methods, docs, interfaces.
**Cons**: must keep PSI classes in sync with grammar manually.

The Elm plugin uses this approach — ~95 rules, hand-written PSI.

### Our approach (TBD)

Start with generated PSI (Option A) for simplicity. Switch to hand-written
(Option B) if we need more control later.

## `extends` — PSI type hierarchy

`extends` makes one rule a subtype of another in the PSI type system. This
collapses intermediate wrapper nodes and enables polymorphic handling:

```bnf
{ extends(".*Expr")=Expression }

Expression ::= IfExpr | LetExpr | BinaryExpr | CallExpr | LiteralExpr | ...

IfExpr ::= IF Expression LBRACE Expression RBRACE ...
BinaryExpr ::= Expression PLUS Expression
LiteralExpr ::= INT | FLOAT | STRING
```

With `extends`, all `*Expr` rules are subtypes of `Expression`. Code that
handles `Expression` can receive any of them without casting.

**Operator precedence**: determined by ORDER in the parent rule. Earlier =
lower precedence. GrammarKit uses a priority-climbing algorithm for
left-recursive rules listed directly in the parent.

**Critical**: all child rules must be listed DIRECTLY in the parent rule —
no `private` intermediate groups, or GrammarKit's left-recursion handling
breaks (infinite loop).

## `fake` rules — PSI types without parser rules

A `fake` rule generates PSI interfaces/classes but NO parser code. Useful for
abstract concepts:

```bnf
fake BinaryExpr ::= Expression + {
    methods=[left="/Expression[0]"  right="/Expression[1]"]
}
```

Now concrete rules like `AddExpr`, `MulExpr` can extend `BinaryExpr` and
share `getLeft()`/`getRight()` accessors.

## `meta` rules — parameterized rules

For reusable patterns like comma-separated lists:

```bnf
private meta comma_separated_list ::= <<param>> (',' <<param>>)* ','?

// Usage:
ArgumentList ::= '(' <<comma_separated_list Expression>> ')'
ParameterList ::= '(' <<comma_separated_list Parameter>> ')'
```

## External rules

For parse decisions that require custom logic (lookahead, context flags):

```bnf
private async ::= <<asyncKeyword>>
NoStructLitExpr ::= <<exprMode 'StructLiteralsMode.OFF' Expr>>
```

These call methods in a `parserUtilClass` (hand-written Kotlin/Java).

## Performance: lazy block parsing

For large files, parsing every function body upfront is expensive. The Rust
plugin uses lazy parsing for blocks:

```bnf
private ShallowBlock ::= <<parseCodeBlockLazy>>
```

This skips the block contents during initial parse — only parsed when the user
opens the file. Stubs only store top-level declarations.

## Testing strategy

### Snapshot tests (gold file comparison)

Same approach as lexer tests. Each test has:
- `LetBinding.res` — source input
- `LetBinding.out` — expected PSI tree dump

The test calls `createPsiFile` + `toParseTreeText` + `assertSameLinesWithFile`.

### Why we use currying for parser tests

`ParsingTestCase` exposes its methods as `protected`, forcing subclass
inheritance. We work around this by capturing the protected methods as lambdas
from inside the subclass:

```kotlin
// ParserTestUtils.kt — standalone function, no inheritance
fun createParserTest(
    createAndSetPsiFile: (String) -> PsiFile,
    ensureNoErrorElements: () -> Unit,
    toParseTreeText: (PsiFile) -> String,
    testDataPath: String,
    fullDataPath: String,
): (String, String) -> Unit = { inputFile, expectedOutputFile ->
    val psiFile = createAndSetPsiFile(inputFile)
    ensureNoErrorElements()
    val actual = toParseTreeText(psiFile)
    // ... compare against gold file
}
```

The test class captures the protected methods once:

```kotlin
class ReScriptParserTest : ParsingTestCase(...) {
    private fun runParserTest(inputFile: String, expectedOutputFile: String) =
        createParserTest(
            createAndSetPsiFile = { file ->
                createPsiFile(file.removeSuffix(".res"), loadFile(file)).also { myFile = it }
            },
            ensureNoErrorElements = ::ensureNoErrorElements,
            toParseTreeText = { toParseTreeText(it, false, false) },
            testDataPath = testDataPath,
            fullDataPath = myFullDataPath,
        )(inputFile, expectedOutputFile)

    fun testLetBinding() = runParserTest("LetBinding.res", "LetBinding.out")
}
```

This is a standard FP pattern (dependency injection via function parameters)
applied to work around Java's `protected` visibility. The test logic lives in
a standalone function; the subclass is just a thin adapter.

### Complete vs. Partial test suites (Elm/Rust pattern)

Both the Elm and Rust plugins split parser tests into two suites:

- **`complete/`** — valid source code. Assert that parsing produces **zero**
  `PsiErrorElement` nodes. If any error appears, the test fails.
- **`partial/`** — intentionally broken source code. Assert that parsing
  produces **at least one** `PsiErrorElement`. Verifies error recovery works
  correctly.

This ensures:
1. Valid code always parses cleanly (complete suite)
2. Invalid code degrades gracefully, not catastrophically (partial suite)

We should adopt this split when we build the real parser.

## Reference implementations

### Elm plugin (`tmp/intellij-elm/`)

- ~95 rules, 522 lines
- `generate = [ psi = "no" ]` — hand-written PSI classes
- Expressions fully parsed with operator precedence via `extends`
- Uses virtual tokens from layout lexer for whitespace sensitivity
- `pin` + `recoverWhile` on all declarations
- `elementTypeFactory` for stub-backed PSI on key declarations

### Rust plugin (`tmp/intellij-rust/`)

- ~365 rules (195 public, 170 private/fake/meta)
- Full PSI generation with `mixin` and `stubClass`
- **Zero LSP usage** — all IDE features (completion, go-to-def, diagnostics,
  type inference) implemented natively via PSI
- Lazy block parsing for performance
- Context flags via bit-masks for ambiguity resolution
- 15 distinct `recoverWhile` rules
- ~67 complete tests + ~20 partial tests
- `_first` lookahead rules for recovery predicates and performance
- `meta` rules for reusable patterns (comma-separated lists)
- `LEXER_VERSION` / `PARSER_VERSION` constants for stub cache invalidation

### Tree-sitter-rescript grammar (reference spec)

- ~130 named rules, ~170 total
- Top-level: `let`, `type`, `module`, `open`, `external`, `exception`, `include`
- Rich type system: variants, records, polymorphic variants, generics, objects
- Operator precedence from lowest to highest: mutation < ternary < `||` < `&&`
  < comparison < `+`/`-` < `**` < `*`/`/` < pipe < await < call < member < unary
- Patterns: destructuring, variant patterns, or-patterns, range patterns
- JSX syntax support
- `let rec ... and ...` for mutual recursion
- `export` modifier on declarations

## Recommended approach for ReScript

### Phase 1: Top-level declarations only

Start with rules for the structural skeleton. Expressions stay flat (consume
all tokens until the next declaration boundary). This gives us structure view,
folding, and go-to-symbol immediately.

```bnf
File ::= item*

private item ::= declaration
    { recoverWhile=item_recover }

private item_recover ::= !(LET | TYPE | MODULE | OPEN | EXTERNAL | <<eof>>)

private declaration ::= LetDeclaration
                       | TypeDeclaration
                       | ModuleDeclaration
                       | OpenStatement
                       | ExternalDeclaration
```

### Phase 2: Expression hierarchy

Add the `Expression` rule with operator precedence via `extends`. Use the
tree-sitter-rescript grammar as the spec for precedence levels.

### Phase 3: Patterns and types

Add pattern matching rules and type expression rules. These are needed for
proper destructuring support and type annotation parsing.

### Future: Lazy parsing, stubs, indexes

Once the grammar is mature, add lazy block parsing for performance and
stub-based indexes for cross-file features (find usages, rename).
