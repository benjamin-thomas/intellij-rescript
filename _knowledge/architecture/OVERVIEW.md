# Architecture — Project structure and design decisions

## Project structure

Use `ls` or `find` for the full file listing. High-level layout:

```
src/main/
  kotlin/.../
    ReScriptLanguage.kt, ReScriptFileType.kt, ReScriptFile.kt, Icons.kt
    lang/           -- Lexer (.flex), parser (.bnf), syntax highlighter, editor features
                       (commenter, brace matcher, quote handler, folding, breadcrumbs,
                       statement mover, goto related, spellchecker, inspection suppressor)
    lang/psi/       -- ElementManipulators for language injection
    lang/psi/impl/  -- PSI mixins (LetBinding, ModuleBinding, TypeDeclaration,
                       StringLiteral, TemplateLiteral)
    lsp/            -- LSP4IJ integration (server factory, parameter info handler)
  gen/              -- Generated code (JFlex lexer, GrammarKit parser + PSI)
  grammars/         -- ReScript.bnf (GrammarKit parser grammar)
  resources/        -- plugin.xml, icons

src/test/
  kotlin/.../       -- Snapshot tests (lexer, parser), editor feature tests
  resources/.../    -- Test fixtures (.res + .out gold file pairs)
```

## Build setup

- **Gradle**: 8.13
- **Kotlin**: 2.3.20
- **IntelliJ Platform Plugin**: 2.11.0
- **GrammarKit**: 2023.3.0.3 (standalone plugin; 2022.3.2.2 had classpath issues with IntelliJ 2025.3)
- **LSP4IJ**: 0.19.2
- **Target IDE**: IntelliJ IDEA 2025.3 (`sinceBuild=253`, no `untilBuild`)
- **Java**: 21 (source + target)

### Key Gradle tasks

- `./gradlew generateLexer` — runs JFlex on `ReScript.flex`
- `./gradlew generateParser` — runs GrammarKit on `ReScript.bnf`
- `./gradlew test` — runs all tests (lexer + parser generation happens automatically)
- `./gradlew runIde` — launches a sandboxed IntelliJ with the plugin installed
- `./gradlew verifyPluginProjectConfiguration` — checks plugin.xml and build config

### runIde usage

```bash
IDEA_JVM_ARGS="-Dsun.java2d.uiScale.enabled=false" \
IDEA_PROJECT=~/code/github.com/benjamin-thomas/7guis/rescript-7guis \
./gradlew runIde
```

### Sandbox mounts (.sandbox-mounts)

Claude Code's sandbox needs access to:
- `~/.gradle` (rw) — Gradle caches
- Reference projects (ro) — Haskell, Elm, Rust plugins
- ReScript test project (ro) — for finding `rescript-language-server`

## Design decisions

### Kotlin objects for singletons

`ReScriptLanguage`, `ReScriptFileType`, `ReScriptInterfaceFileType` are Kotlin
`object` singletons (not `class` + `companion object { INSTANCE }`).

IntelliJ's DevKit inspections require:
- `fieldName="INSTANCE"` in plugin.xml for FileType objects
- `readResolve()` on Language objects (because `Language` implements `Serializable`)
- No companion objects with non-constant values in extension classes

### LSP integration

The ReScript language server (`@rescript/language-server`) is found via PATH
(globally installed with `npm install -g`). If not found, a balloon notification
tells the user how to install it. The notification fires only once (guarded by
`AtomicBoolean`), and `LanguageServerManager.stop()` is called to prevent
LSP4IJ from retrying.

The LSP provides: hover, completion, go-to-definition, diagnostics, formatting,
code actions. It does NOT provide inlay hints / inline type annotations — this
is a ReScript language server limitation (unlike OCaml's `merlin`).

### Testing philosophy

**Snapshot testing**: Both lexer and parser tests compare actual output against
gold files (`.out`). If the gold file doesn't exist, `assertSameLinesWithFile`
auto-creates it and fails — like Jest snapshots.

**No unnecessary inheritance**: Lexer tests use standalone functions
(`runSnapshotTest` in `LexerTestUtils.kt`) instead of extending `LexerTestCase`.
Parser tests are forced to extend `ParsingTestCase` (because it sets up IntelliJ's
mock application), but test logic is extracted into a curried standalone function
(`createParserTest` in `ParserTestUtils.kt`) that receives the protected methods
as lambdas.

**Why currying for parser tests**: `ParsingTestCase`'s methods are `protected` —
they can't be called from external functions. We capture them as lambdas from
inside the subclass, then pass them to a standalone function. This keeps the
test logic readable and explicit, with the inheritance boundary contained to
one place.

**Incremental lexing tests**: `checkZeroState` and `checkCorrectRestart` verify
the lexer works correctly when IntelliJ re-lexes from the middle of a file
(which happens on every keystroke).

## What the PSI Viewer shows

The PSI Viewer (Tools > View PSI Structure in runIde) shows the structured tree
produced by the GrammarKit parser:

```
ReScript File
  ReScriptLetBindingImpl(LET_BINDING)
    PsiElement(LET)('let')
    ReScriptLetBindingPatternImpl(LET_BINDING_PATTERN)
      PsiElement(LIDENT)('x')
    PsiElement(EQ)('=')
    ReScriptExprImpl(EXPR)
      PsiElement(INT)('1')
```

## Layers and what they provide

| Layer | Technology | What it gives | Needs server? |
|---|---|---|---|
| Lexer | JFlex | Syntax coloring | No |
| Parser + PSI | GrammarKit | Structure view, folding, go-to-symbol, breadcrumbs | No |
| LSP | LSP4IJ + rescript-language-server | Hover, completion, go-to-def, diagnostics, formatting | Yes |

The lexer and parser work offline/instantly. The LSP requires the language
server process to be running. Both are valuable — the lexer gives immediate
visual feedback, the LSP gives semantic intelligence.
