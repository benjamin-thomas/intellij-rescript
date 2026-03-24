# intellij-rescript — ReScript Plugin for JetBrains IDEs

## What this is

A high-quality, open-source ReScript plugin for JetBrains IDEs (IntelliJ,
WebStorm, etc.) using the same architecture as
[intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp):

- **GrammarKit** for parsing/PSI (syntax highlighting, structure, inspections)
- **LSP4IJ** for all semantic intelligence (completion, navigation, diagnostics)
- **Thin glue code** connecting the two

The ReScript LSP (`@rescript/language-server`) is fast and well-maintained
(OCaml-based compiler underneath). We delegate all heavy semantic work to it.

## How we work

**TDD-first, checkpoint-driven, no vibe coding.**

Every feature follows RED-GREEN-REFACTOR:
1. Write a failing test that describes the expected behavior
2. Write minimal code to make it pass
3. Refactor if needed, keeping tests green
4. Stop and review before moving on

IntelliJ's test framework supports this well. Key test base classes:
- `LexerTestCase` — feed source text, assert token stream
  (see [HaskellLexerTest.kt](https://github.com/rockofox/intellij-haskell-lsp/blob/main/src/test/kotlin/boo/fox/haskelllsp/language/HaskellLexerTest.kt))
- `ParsingTestCase` — feed source text, assert PSI tree structure as `.txt` snapshots
  (see [HaskellParserTest.kt](https://github.com/rockofox/intellij-haskell-lsp/blob/main/src/test/kotlin/boo/fox/haskelllsp/language/HaskellParserTest.kt))
- `BasePlatformTestCase` — test plugin features (inspections, completion, etc.)

The TDD prompt at `~/.pi/agent/prompts/tdd.md` defines the exact workflow:
RED phase → report → wait for approval → GREEN phase → report → wait for
approval → state next test intention → repeat.

**The goal is learning, not shipping fast.** Each chunk should be small enough
to understand fully. We advance in deliberate steps, not in a blur.

## Reference material

- [intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp) —
  Architecture template. Local copy at
  `/home/benjamin/code/github.com/rockofox/intellij-haskell-lsp`.
  Study its `build.gradle.kts`, `plugin.xml`, lexer, parser, and test setup.
- [tree-sitter-rescript grammar.js](https://github.com/rescript-lang/tree-sitter-rescript/blob/main/grammar.js) —
  ~140 rules, ~1,100 lines. Our reference for building the GrammarKit BNF.
- [ReScript compiler parser (res_core.ml)](https://github.com/rescript-lang/rescript/blob/master/compiler/syntax/src/res_core.ml) —
  Grammar rules embedded as comments (search for `::=`).
- [intellij-elm](https://github.com/intellij-elm/intellij-elm) —
  Gold-standard IntelliJ language plugin. Study for architecture patterns, test
  structure (432 test files), and how to build a complete PSI.
- [JetBrains plugin template](https://github.com/JetBrains/intellij-platform-plugin-template) —
  Modern project scaffold. Local copy at
  `/home/benjamin/code/github.com/benjamin-thomas/intellij-plugin-example`.
- [Custom Language Support Tutorial](https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html) —
  Official step-by-step guide for custom language plugins.
- [GrammarKit](https://github.com/JetBrains/Grammar-Kit) —
  BNF syntax reference and parser generation docs.
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) —
  The LSP client framework we use. Check its `plugin.xml` for available
  extension points (call hierarchy, type hierarchy, folding, etc.).

## Project setup

### Starting point

Copy the scaffold from the JetBrains plugin template at
`/home/benjamin/code/github.com/benjamin-thomas/intellij-plugin-example`.

Key files to bring over:
```
build.gradle.kts
gradle.properties
settings.gradle.kts
gradle/                  # wrapper + version catalog (libs.versions.toml)
gradlew / gradlew.bat
.gitignore
```

Then adapt:

**`gradle.properties`** — change plugin identity, target a modern IDE:
```properties
pluginGroup = com.github.benjaminthomas.intellijrescript
pluginName = intellij-rescript
pluginRepositoryUrl = https://github.com/benjamin-thomas/intellij-rescript
pluginVersion = 0.1.0
pluginSinceBuild = 243
pluginUntilBuild = 252.*
platformType = IC
platformVersion = 2024.3
platformPlugins = com.redhat.devtools.lsp4ij:0.13.0
```

**`settings.gradle.kts`** — rename the project:
```kotlin
rootProject.name = "intellij-rescript"
```

**`build.gradle.kts`** — add GrammarKit plugin and `runIde` args. See
intellij-haskell-lsp's `build.gradle.kts` for the `grammarKit` block pattern.
Add:
```kotlin
tasks {
    runIde {
        jvmArgs("-Dsun.java2d.uiScale.enabled=false")
    }
}
```

Targeting `platformVersion = 2024.3` gives you the new UI in `runIde`.

## Implementation phases

Each phase is a series of TDD cycles. Each cycle produces a test + minimal
implementation. We don't move to the next phase until all tests pass and we
understand what we've built.

---

### Phase 0 — Skeleton

**Goal**: Plugin loads, `.res`/`.resi` files are recognized, tests run.

TDD cycles:
1. **Test**: `ReScriptFileTypeTest` — assert `.res` maps to `ReScriptFileType`
2. **Implement**: `ReScriptLanguage.kt`, `ReScriptFileType.kt`, `ReScriptFile.kt`
3. **Test**: assert `.resi` also maps to the same language
4. **Implement**: register both extensions in `plugin.xml`
5. **Manual check**: `./gradlew runIde`, open a `.res` file, see the icon

Files created:
```
src/main/kotlin/.../ReScriptLanguage.kt       (~5 lines)
src/main/kotlin/.../ReScriptFileType.kt        (~15 lines)
src/main/kotlin/.../ReScriptFile.kt            (~5 lines)
src/main/kotlin/.../Icons.kt                   (~5 lines)
src/main/resources/META-INF/plugin.xml
src/main/resources/icons/rescript.svg
src/test/kotlin/.../ReScriptFileTypeTest.kt
```

---

### Phase 1 — Lexer + syntax highlighting

**Goal**: ReScript source files have proper syntax coloring.

TDD cycles (one per token category):
1. **Test**: keywords — `let` → `LET`, `type` → `TYPE`, `module` → `MODULE`, etc.
2. **Test**: identifiers — `foo` → `LIDENT`, `Foo` → `UIDENT`
3. **Test**: literals — `"hello"` → `STRING`, `42` → `INT`, `3.14` → `FLOAT`
4. **Test**: comments — `// line` → `COMMENT`, `/* block */` → `BLOCK_COMMENT`
5. **Test**: operators — `->` → `ARROW`, `=>` → `FAT_ARROW`, `|>` → `PIPE`
6. **Test**: delimiters — `(`, `)`, `{`, `}`, `[`, `]`
7. **Test**: decorators — `@module` → `AT` + `LIDENT`
8. **Test**: special — `...` → `DOTDOTDOT`, `~` → `TILDE`
9. **Test**: template strings — `` `hello ${name}` ``

Each test uses `LexerTestCase.doTest(input, expectedTokenStream)`.

Then wire up `ReScriptSyntaxHighlighter.kt` mapping tokens to colors.

Files created:
```
src/main/kotlin/.../language/ReScript.flex       (~300 lines)
src/main/kotlin/.../language/ReScriptLexer.kt    (~5 lines, adapter)
src/main/kotlin/.../language/ReScriptTokenType.kt (~5 lines)
src/main/kotlin/.../language/ReScriptSyntaxHighlighter.kt  (~80 lines)
src/main/kotlin/.../language/ReScriptSyntaxHighlighterFactory.kt  (~5 lines)
src/test/kotlin/.../language/ReScriptLexerTest.kt
```

---

### Phase 2 — Parser + PSI

**Goal**: GrammarKit parser produces typed PSI nodes for declarations.

TDD approach: for each grammar rule, create a `.res` test fixture and a `.txt`
file with the expected PSI tree (snapshot testing via `ParsingTestCase`).

TDD cycles:
1. **Test**: `let x = 1` → PSI tree with `LetDeclaration` node
2. **Test**: `let add = (x, y) => x + y` → `LetDeclaration` with params
3. **Test**: `type t = int` → `TypeDeclaration` node
4. **Test**: `type color = Red | Blue | Green` → `TypeDeclaration` with variants
5. **Test**: `type user = {name: string, age: int}` → record type
6. **Test**: `module Foo = { ... }` → `ModuleDeclaration` node
7. **Test**: `open Belt` → `OpenStatement` node
8. **Test**: `external log: string => unit = "console.log"` → `ExternalDeclaration`
9. **Test**: `@react.component let make = ...` → decorator + declaration
10. **Test**: `import "qualified" as X` → `ImportDeclaration`
11. **Test**: full file with multiple declarations → correct tree structure
12. **Test**: expressions stay as flat token sequences (intentional)

Files created:
```
src/main/kotlin/.../language/rescript.bnf          (~200 lines)
src/main/kotlin/.../language/ReScriptParserDefinition.kt  (~30 lines)
src/main/kotlin/.../language/ReScriptParserUtil.kt  (if needed)
src/test/kotlin/.../language/ReScriptParserTest.kt
src/test/testData/parser/*.res                      (test fixtures)
src/test/testData/parser/*.txt                      (expected PSI trees)
```

The BNF doesn't need to cover the full language. Focus on declarations and
top-level structure. Expressions can be flat. The
[tree-sitter-rescript grammar](https://github.com/rescript-lang/tree-sitter-rescript/blob/main/grammar.js)
is the reference, but we only need ~60-80 rules (not all 140).

---

### Phase 3 — LSP integration

**Goal**: Full IDE experience via the ReScript language server.

This phase is less TDD-friendly (LSP integration is mostly wiring), but we can
still test:
1. **Test**: `ReScriptLanguageServerFactory` creates a valid connection provider
2. **Test**: LSP path resolution (project-local `node_modules` → PATH fallback)
3. **Manual test**: `./gradlew runIde`, open a ReScript project, verify:
   - Completion works
   - Go-to-definition works
   - Diagnostics appear
   - Formatting works (Ctrl+Alt+L)

Files created:
```
src/main/kotlin/.../ReScriptLanguageServerFactory.kt   (~40 lines)
src/main/kotlin/.../settings/ReScriptSettings.kt       (~30 lines)
src/main/kotlin/.../settings/ReScriptSettingsConfigurable.kt  (~60 lines)
src/test/kotlin/.../ReScriptLanguageServerFactoryTest.kt
```

The LSP server factory locates `rescript-language-server`:
1. User-configured path in settings
2. `node_modules/.bin/rescript-language-server` relative to project root
3. PATH fallback

Launch with `--stdio`.

Register LSP4IJ extension points in `plugin.xml` (one line each):
```xml
<callHierarchyProvider language="ReScript"
    implementationClass="com.redhat.devtools.lsp4ij.features.callHierarchy.LSPCallHierarchyProvider"/>
<typeHierarchyProvider language="ReScript"
    implementationClass="com.redhat.devtools.lsp4ij.features.typeHierarchy.LSPTypeHierarchyProvider"/>
<lang.foldingBuilder language="ReScript"
    implementationClass="com.redhat.devtools.lsp4ij.features.foldingRange.LSPFoldingRangeBuilder"
    order="first"/>
<codeInsight.parameterInfo language="ReScript"
    implementationClass="com.redhat.devtools.lsp4ij.features.signatureHelp.LSPParameterInfoHandler"/>
<lang.psiStructureViewFactory language="ReScript"
    implementationClass="com.redhat.devtools.lsp4ij.features.documentSymbol.LSPDocumentSymbolStructureViewFactory"/>
```

---

### Phase 4 — Polish

**Goal**: Small features that make the plugin feel complete.

Each is a self-contained TDD cycle:

| Feature | Test approach | Lines |
|---|---|---|
| Brace matcher `()[]{}` | Test via `CodeInsightTestFixture` | ~15 |
| Commenter `//` and `/* */` | `CommenterTest` — toggle comment | ~15 |
| Quote handler | Type `"`, assert `""` with cursor between | ~20 |
| Live templates | XML only, manual test | ~50 XML |
| File templates | Manual test | ~30 |
| Color settings page | Manual test | ~80 |
| Breadcrumbs | `BreadcrumbsTest` with fixture | ~60 |

---

### Phase 5 — PSI-powered native features

**Goal**: Leverage the PSI tree for features that don't need LSP.

| Feature | Test approach |
|---|---|
| Native structure view | `StructureViewTest` — assert tree nodes |
| Go-to-symbol (Ctrl+Alt+Shift+N) | Fixture with declarations, assert found |
| Unused `open` inspection | Fixture with used/unused opens, assert warnings |
| Missing type annotation | Fixture with annotated/unannotated lets |
| Native folding | `FoldingTest` — assert fold regions |

---

## Estimated size

| Component | Lines (approx) |
|---|---|
| Lexer (JFlex) | ~300 |
| Parser (BNF) | ~200 (generates ~5,000-10,000 lines of PSI) |
| Glue code (Kotlin) | ~500 |
| Tests (Kotlin + fixtures) | ~600 |
| Polish features | ~400 |
| PSI-powered features | ~500 |
| **Total hand-written** | **~2,500** |

Small. Maintainable. Well-tested.

## File type details

| Extension | Language ID (LSP) | Description |
|---|---|---|
| `.res` | `rescript` | ReScript source |
| `.resi` | `rescript` | ReScript interface |

## LSP details

- **npm package**: [`@rescript/language-server`](https://www.npmjs.com/package/@rescript/language-server)
- **Binary**: typically at `node_modules/.bin/rescript-language-server`
- **Launch args**: `--stdio`
- **Latest version**: 1.72.0 (Jan 2026)
- **Capabilities**: completion, hover, definition, references, rename,
  diagnostics, formatting, code actions, semantic tokens

## What NOT to build

- **Type checker** — the ReScript compiler + LSP handles this
- **Build system integration** — `rescript build` from terminal is fine
- **Deep expression parsing** — keep expressions flat, let LSP do semantics
- **Package manager UI** — npm/yarn/pnpm work fine
- **Debugger / REPL** — out of scope for now