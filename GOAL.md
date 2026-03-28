# intellij-rescript — ReScript Plugin for JetBrains IDEs

## What this is

A ReScript plugin for JetBrains IDEs (IntelliJ, WebStorm, etc.). Architecture
inspired by [intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp),
[intellij-elm](https://github.com/elm-tooling/intellij-elm), and
[intellij-rust](https://github.com/intellij-rust/intellij-rust):

- **GrammarKit** for parsing/PSI (syntax highlighting, structure, inspections)
- **LSP4IJ** for all semantic intelligence (completion, navigation, diagnostics)
- **Thin glue code** connecting the two

The ReScript LSP (`@rescript/language-server`) handles semantic work
(OCaml-based compiler underneath).

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
  `~/code/github.com/rockofox/intellij-haskell-lsp`.
  Study its `build.gradle.kts`, `plugin.xml`, lexer, parser, and test setup.
- [tree-sitter-rescript grammar.js](https://github.com/rescript-lang/tree-sitter-rescript/blob/main/grammar.js) —
  ~140 rules, ~1,100 lines. Our reference for building the GrammarKit BNF.
- [ReScript compiler parser (res_core.ml)](https://github.com/rescript-lang/rescript/blob/master/compiler/syntax/src/res_core.ml) —
  Grammar rules embedded as comments (search for `::=`).
- [intellij-elm](https://github.com/elm-tooling/intellij-elm) —
  Mature IntelliJ language plugin, good reference. Local copy at
  `~/code/github.com/elm-tooling/intellij-elm`.
  Study for architecture patterns, test structure, and how to build a complete PSI.
- [intellij-rust](https://github.com/intellij-rust/intellij-rust) —
  Archived, good reference. Local copy at
  `~/code/github.com/intellij-rust/intellij-rust`.
  Thorough test suite, well-structured lexer/parser.
- [JetBrains plugin template](https://github.com/JetBrains/intellij-platform-plugin-template) —
  Modern project scaffold. Local copy at
  `~/code/github.com/benjamin-thomas/intellij-plugin-example`.
- [Custom Language Support Tutorial](https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html) —
  Official step-by-step guide for custom language plugins. Start here.
- [Custom Language Support Reference](https://plugins.jetbrains.com/docs/intellij/custom-language-support.html) —
  Comprehensive reference for every extension point (lexer, parser, formatter,
  commenter, brace matching, structure view, find usages, refactoring, etc.).
- [PSI (Program Structure Interface)](https://plugins.jetbrains.com/docs/intellij/psi.html) —
  The typed AST used by most IntelliJ plugin features.
- [Testing Plugins](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html) —
  Test infrastructure, base classes, test data conventions.
- [GrammarKit](https://github.com/JetBrains/Grammar-Kit) —
  BNF syntax reference and parser generation docs.
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) —
  The LSP client framework we use. Check its `plugin.xml` for available
  extension points (call hierarchy, type hierarchy, folding, etc.).
- [LSP4IJ Developer Guide](https://github.com/redhat-developer/lsp4ij/blob/main/docs/DeveloperGuide.md) —
  How to wire up LSP servers, extension points, client features.

## Developer tools

### Inspecting the PSI tree

Two ways to view the PSI tree structure while developing:

1. **PsiViewer plugin** (recommended) — install from the JetBrains marketplace:
   https://plugins.jetbrains.com/plugin/227-psiviewer
   Gives you a persistent tool window. Works in any IntelliJ instance. This is
   what the [Elm plugin recommends](https://github.com/elm-tooling/intellij-elm/blob/main/docs/contributing.md).

2. **Built-in PSI Viewer** — requires enabling IntelliJ's internal mode:
   - Help > Edit Custom Properties, add `idea.is.internal=true`, restart
   - Then: Tools > View PSI Structure
   - Also unlocks Internal Actions menu, UI Inspector, and other debug tools

### Debugging the plugin

- **IDE log**: `tail -f build/idea-sandbox/IU-2025.3/log/idea.log` — shows
  errors, warnings, and any `Logger.getInstance("ReScript").warn(...)` output
  in real-time while the sandboxed IDE is running.
- **LSP traces**: In `runIde`, go to Settings > Languages & Frameworks >
  Language Servers > select ReScript > Debug tab > set trace to verbose.
  Then View > Tool Windows > Language Servers > LSP Consoles > Traces tab
  shows full JSON request/response between IDE and LSP server.
- **Debugger**: In your development IDE, find `runIde` in the Gradle tool
  window (Tasks > intellij platform > runIde), right-click > Debug. This
  launches the sandboxed IDE with the debugger attached — set breakpoints
  in your plugin code as normal. Note: the development IDE must be started
  from a shell that has `rescript-language-server` on its PATH.

### Custom parameter info handler

LSP4IJ's built-in `LSPParameterInfoHandler` does not render the `documentation`
field from the LSP `signatureHelp` response. The ReScript LSP server sends rich
documentation (markdown with examples, MDN links), but LSP4IJ's `updateUI`
method only reads the `label` and `parameters` fields.

We wrote `ReScriptParameterInfoHandler` to fix this. It reuses LSP4IJ's
infrastructure for fetching (`LSPFileSupport`, `LSPSignatureHelpSupport`,
`LSPSignatureHelperPsiElement`) and only customizes the rendering in `updateUI`.

Key issue found during development: the `documentation` field is wrapped in an
`Either<String, MarkupContent>` object. Casting it directly to `MarkupContent`
or `String` returns null — you must unwrap the `Either` first.

**Future improvement**: render the documentation as HTML with markdown formatting
and syntax-highlighted code blocks. Currently shown as plain text because
`setupUIComponentPresentation` only supports single-style text. Rich rendering
would require a custom `JComponent` popup (~200-300 lines).

## Documentation

- **`ARCHITECTURE.md`** — project structure, build setup, design decisions, testing philosophy
- **`LEXER.md`** — how JFlex works, token types, syntax highlighting, lexer testing
- **`PARSER.md`** — how GrammarKit works, error recovery (pin/recoverWhile),
  PSI generation, parser testing, reference implementations (Elm, Rust, tree-sitter-rescript)

## Project setup

See `ARCHITECTURE.md` for current build setup, project structure, and design
decisions. See `build.gradle.kts` for exact versions.

## Implementation phases

Each phase is a series of TDD cycles. Each cycle produces a test + minimal
implementation. We don't move to the next phase until all tests pass and we
understand what we've built.

---

### Phase 0 — Skeleton (DONE)

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

**Goal**: ReScript source files have proper syntax coloring in `runIde`.

Strategy: implement a basic lexer first (enough for visible syntax coloring), then
incrementally expand to cover the full ReScript syntax. The lexer design must not
paint us into a corner — keep token types general enough to accommodate future
syntax without breaking changes.

#### Phase 1a — Basic lexer (DONE — get to `runIde` with colors)

TDD cycles (one per token category):
1. **Test**: keywords — `let` → `LET`, `type` → `TYPE`, `module` → `MODULE`, etc.
2. **Test**: identifiers — `foo` → `LIDENT`, `Foo` → `UIDENT`
3. **Test**: literals — `"hello"` → `STRING`, `42` → `INT`, `3.14` → `FLOAT`
4. **Test**: comments — `// line` → `LINE_COMMENT`, `/* block */` → `BLOCK_COMMENT`
   **TODO**: block comments use a simple regex — doesn't handle nested `/* /* */ */`.
   ReScript inherits OCaml's nested block comments. Fix later with a JFlex state
   and depth counter (see Haskell plugin's `NCOMMENT` state for reference).
5. **Test**: operators — `->` → `ARROW`, `=>` → `FAT_ARROW`, `|>` → `PIPE`, `=` → `EQ`
6. **Test**: delimiters — `(`, `)`, `{`, `}`, `[`, `]`
7. **Test**: decorators — `@module` → `AT` + `LIDENT`
8. **Test**: special — `...` → `DOTDOTDOT`, `~` → `TILDE`
9. Wire up `ReScriptSyntaxHighlighter.kt` mapping tokens to colors.
10. Manual check: `runIde`, open a `.res` file, see colored syntax.

#### Phase 1b — Full numeric literals

ReScript has rich numeric syntax (reference: tree-sitter-rescript grammar.js):
- Underscore separators: `1_000_000`, `0xa_b_c`
- Leading-dot floats: `.123`, `.4_5e6`
- Hex: `0xFF`, `0xA_B_C` (also hex floats: `0x1.2p+10`)
- Octal: `0o77`, `0o1_1`
- Binary: `0b1010`, `0b1_000_000`
- Exponents: `1e10`, `3.14e-2`
- BigInt suffix: `42n`, `0xFFn`
- int32/int64 suffix: `10l`, `1L`
- Signed literals: `-3`, `+3.0`

#### Phase 1c — Template strings

Backtick template strings with interpolation: `` `hello ${name}` ``

#### Phase 1d — v12 operator tokens

New tokens introduced in ReScript v12:
- Bitwise (F# style): `&&&`, `|||`, `^^^`, `~~~`
- Shift: `<<`, `>>`, `>>>`
- Regex literals: `/pattern/flags` (context-sensitive, like JS)
- `dict{` syntax (contextual keyword)
- `let?` binding (experimental)
- Deprecated but still valid: `+.`, `-.`, `*.`, `/.`, `++`

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

### Phase 3 — LSP integration (DONE — basic wiring)

**Goal**: Full IDE experience via the ReScript language server.

**Completed**: LSP4IJ wiring with `@rescript/language-server` (globally installed
via `npm install -g`). Hover, completion, go-to-definition, diagnostics, and
formatting all work. Error notification with install instructions when server
not found.

**Not yet done**: LSP4IJ extension points for call hierarchy, type hierarchy,
folding, parameter info, structure view. These are one-line XML registrations
in `plugin.xml` that delegate to LSP4IJ's built-in providers:
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

**Note**: The ReScript language server does NOT support inlay hints / inline
type annotations (unlike OCaml's merlin). Types are available via hover only.

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


## File type details

| Extension | Language ID (LSP) | Description |
|---|---|---|
| `.res` | `rescript` | ReScript source |
| `.resi` | `rescript` | ReScript interface |

## LSP details

- **npm package**: [`@rescript/language-server`](https://www.npmjs.com/package/@rescript/language-server)
- **Install**: `npm install -g @rescript/language-server`
- **Our plugin finds it**: via PATH lookup
- **Launch args**: `--stdio`
- **Capabilities**: completion, hover, definition, references, rename,
  diagnostics, formatting, code actions, semantic tokens
- **Not supported**: inlay hints / inline type annotations (unlike OCaml/F#)
- **Relationship to `rescript-editor-analysis.exe`**: The LSP server (Node.js)
  internally calls `rescript-editor-analysis.exe` (native OCaml binary bundled
  with the `rescript` npm package) for analysis. The LSP server is a thin
  wrapper around the analysis binary.

## What NOT to build (for now)

- **Type checker** — the ReScript compiler + LSP handles this
- **Build system integration** — `rescript build` from terminal is fine
- **Deep expression parsing** — keep expressions flat, let LSP do semantics
- **Package manager UI** — npm/yarn/pnpm work fine
- **Debugger / REPL** — out of scope for now

## Plugin evolution strategy — LSP first, then native

### The idea

The plugin evolves in two phases for each IDE feature:

1. **LSP first**: Enable the feature via LSP4IJ (usually a one-line XML
   registration). This works immediately with no PSI work. Observe the behavior
   — what works well, what's limited, what's missing.

2. **Native replacement**: Build a PSI-based implementation that provides
   capabilities beyond what LSP can offer. Register it in `plugin.xml` —
   IntelliJ's priority system automatically prefers the native handler over
   LSP4IJ's generic one. No "disable LSP" code needed.

### Why replace LSP features at all?

Not for speed (though native is faster). The real reason is that native PSI
implementations can do things LSP cannot:

- **Deep IDE integration**: IntelliJ features like breadcrumbs, structure view
  with custom icons, smart code folding, and live templates work best with PSI.
- **Cross-file refactoring**: A native rename can walk the PSI trees of all
  project files, resolving references and updating them atomically. LSP rename
  is limited to what the language server supports — for example, some language
  servers cannot rename across module boundaries.
- **Custom inspections**: "Unused open", "missing type annotation", or
  project-specific lint rules that the LSP server doesn't provide.
- **Offline operation**: PSI features work without a running language server.

### How to apply this (for each feature)

```
1. Enable via LSP       →  one-line XML in plugin.xml
2. Use it, evaluate     →  what works? what's missing?
3. Build native version →  uses our PSI tree (from the BNF grammar)
4. Register native      →  IntelliJ prefers it automatically
5. Tighten the grammar  →  refine PSI tree rules as needed for the feature
```

Step 5 is key: the permissive parser lets us start with LSP immediately, then
we tighten specific grammar rules only when a native feature demands it.
Each rule refinement is a small, testable TDD cycle.

### Example: structure view

1. **LSP**: Register `LSPDocumentSymbolStructureViewFactory` in plugin.xml →
   structure view works immediately, showing all symbols with types.
2. **Evaluate**: works well — shows functions, types, modules with correct
   names. But uses generic icons, no custom sorting or filtering.
3. **Native** (future): Write a `PsiStructureViewFactory` that walks
   `LetBinding`, `ModuleBinding`, `TypeDeclaration` PSI nodes. Requires
   tightening the grammar to parse declaration names (e.g.,
   `LetBinding ::= LET LIDENT body_token*` instead of `LET body_token*`).
   Would enable custom icons, sorting, and filtering.
4. **Register**: IntelliJ prefers the native factory over the LSP one.

Note: some features like **code folding** are not supported by the ReScript
LSP server (`foldingRangeProvider` is absent). For those, skip straight to
a native PSI-based implementation.

### Progression of features

| Feature | Start with LSP | Then native (when needed) |
|---|---|---|
| Code folding | LSPFoldingRangeBuilder | PSI-based FoldingBuilder |
| Structure view | LSPDocumentSymbolStructureViewFactory | PSI-based StructureViewFactory |
| Breadcrumbs | LSP (if supported) | PSI-based BreadcrumbsProvider |
| Find usages | LSP references | PSI reference resolution across files |
| Rename | LSP rename | PSI-based rename (cross-module support) |
| Extract variable | LSP code actions | PSI-based RefactoringSupportProvider |
| Custom inspections | (not available via LSP) | PSI-based LocalInspectionTool |