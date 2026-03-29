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

### Tracing an LSP feature end-to-end

Example: **Go to Definition** — Ctrl+click on `withBackLink` (line 78 in
`App.res`, a usage) and the IDE jumps to its definition (line 34).

```
IntelliJ (user Ctrl+clicks on `withBackLink` at line 78)
    │
    ▼
LSP4IJ (builds JSON-RPC request, sends over stdio)
    │  { "method": "textDocument/definition",
    │    "params": { "textDocument": { "uri": "file:///path/to/App.res" },
    │                "position": { "line": 77, "character": 15 } } }
    ▼
server.ts (Node.js — thin router, ~2K lines)
    │  Matches method in if/else chain (line ~1786)
    │  Calls definition() handler (line ~722)
    │  Shells out to the analysis binary via utils.runAnalysisCommand()
    ▼
rescript-editor-analysis.exe (native OCaml binary — the real brain, ~16K lines)
    │  Entry point: analysis/bin/main.ml — pattern matches on argv
    │  "definition" → Commands.definition → References.definitionForLocItem
    │  Reads .cmt files (compiled type info from ReScript compiler)
    │  Returns JSON to stdout
    ▼
server.ts (captures stdout, forwards as JSON-RPC response)
    │  { "result": { "uri": "file:///.../App.res",
    │                "range": { "start": { "line": 33, "character": 4 },
    │                           "end":   { "line": 33, "character": 16 } } } }
    ▼
LSP4IJ (receives response, navigates editor to line 34: `let withBackLink = ...`)
```

**Testing the analysis binary directly** (useful for debugging):

```bash
cd ~/code/github.com/benjamin-thomas/7guis/rescript-7guis

# Make sure the project is compiled first (needs .cmt files)
npx rescript build

# Then query directly (positions are 0-indexed, following LSP)
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe definition src/App.res 77 15
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe hover src/App.res 77 15 true
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe references src/App.res 33 4
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe documentSymbol src/App.res
```

Run the binary with no args to see all available commands.

**Testing the LSP server directly** (demonstrates the full protocol):

```bash
./manage/dev/test-lsp src/App.res 77 15
```

This sends initialize → initialized → didOpen → definition over stdio and
prints the response. The server is stateful (needs initialization + file open
before queries) and needs time to process files after didOpen.

**Key files in the LSP server codebase** (`rescript-vscode` repo):

| File | Role |
|---|---|
| `server/src/server.ts` | JSON-RPC router — dispatches LSP methods to handlers |
| `server/src/utils.ts` | `runAnalysisCommand()` — shells out to the OCaml binary |
| `analysis/bin/main.ml` | CLI entry point — parses argv, calls Commands |
| `analysis/src/Commands.ml` | Feature implementations (definition, hover, etc.) |
| `analysis/src/References.ml` | Symbol resolution for definition/references |
| `analysis/src/SignatureHelp.ml` | Signature help implementation |
| `analysis/src/DocumentSymbol.ml` | Document symbols (used by structure view) |

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

### Code folding (native, PSI-based)

The ReScript LSP server does not support `foldingRangeProvider`, so code folding
is implemented natively via `ReScriptFoldingBuilder`. It walks the PSI tree,
finds matching `LBRACE`/`RBRACE` pairs, and creates fold regions for blocks that
span multiple lines.

Folding tests use IntelliJ's `testFolding()` API with fixture files that contain
`<fold text='...'>` markers. These markers are NOT ReScript syntax — they're
IntelliJ test annotations that specify where folds should appear and what
placeholder text to show when collapsed. The files must keep the `.res` extension
so IntelliJ knows which parser to use. They live in a separate
`folding/fixtures/` directory to avoid confusion with real ReScript files.

### Decorator-declaration association

Currently `@react.component` and the following `let make` are **sibling** nodes
in the PSI tree, not parent-child. This means you can't walk from a `LetBinding`
to its decorators without scanning siblings.

If we ever need this (e.g., for "go to decorated function", code folding that
includes the decorator, or inspections like "missing @react.component"), we'd
wrap them: `DecoratedDeclaration ::= Decorator+ block_declaration`. This is a
grammar change, not an architectural one — the permissive body approach stays
the same.

### Installing the plugin locally

To install the plugin in your real IDE (not the sandboxed `runIde`):

```bash
./gradlew buildPlugin
```

This produces `build/distributions/intellij-rescript-0.1.0.zip`. Then in your IDE:

1. **Settings > Plugins > gear icon > Install Plugin from Disk...**
2. Select the ZIP file
3. Restart the IDE

The plugin requires LSP4IJ to be installed as well (it's a dependency). Install
it from the JetBrains marketplace first if you don't have it.

To update after code changes, rebuild and repeat. No need to uninstall first —
installing from disk replaces the existing version.

## Next steps

### Done in 0.1.0

- Syntax highlighting for all core tokens (keywords, identifiers, literals,
  operators, comments, decorators)
- Permissive parser with all major declaration types (let, type, module, open,
  include, external, exception, decorators, extension points)
- Recursive nesting inside braces
- Regex literal support (`/pattern/flags`) with previous-token disambiguation
- Native code folding (PSI-based, since LSP doesn't support it)
- LSP features: structure view, code lens, parameter info with documentation
- Custom parameter info handler (fixes LSP4IJ's missing documentation rendering)
- LSP server configuration (code lens, build prompt, signature help)
- 33 tests (lexer snapshots, parser snapshots, folding)

### For 0.2.0

1. **Template strings** — backtick strings with `${interpolation}`. Needs a
   JFlex state for the interpolation context.

2. **Missing keywords** — `async`, `await`, `and`, `as`, `try`, `catch`,
   `while`, `for`, `true`, `false`.

3. **Brace matcher** — auto-insert `}` when typing `{`, same for `()`, `[]`.

4. **Commenter** — `//` and `/* */` toggle via Ctrl+/.

5. **Nested block comments** — `/* /* */ */` requires a JFlex state with depth
   counter.

6. **Regex internals highlighting** — break `/pattern/flags` into multiple
   tokens (delimiters, character classes, negation, flags) for richer
   syntax coloring, like WebStorm's JavaScript regex highlighting. Requires
   expanding the REGEX JFlex state from one rule to ~30-40 lines with 5-6
   new token types.

### Medium term

7. **Tighten LetBinding to extract the name** — change `LET body_token*` to
   `LET LIDENT body_token*`. Enables native structure view with declaration
   names.

8. **Native structure view** — once names are extractable, build a
   `PsiStructureViewFactory` with custom icons. Replaces the LSP one.

9. **Rich parameter info rendering** — render markdown documentation as HTML
   in the popup instead of plain text (~200-300 lines).

### Longer term

10. **Expression parsing** — start tightening `body_token*` into real
    expression rules. Fixes the greedy binding problem and enables refactoring.

11. **Decorator-declaration association** — wrap `Decorator+ declaration` into
    a parent node for refactoring and inspection support.

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
- **Capabilities**: completion, hover, definition, typeDefinition, references,
  rename, diagnostics, formatting, code actions, semantic tokens, signature help,
  document symbols, inlay hints, code lens
- **Not supported**: folding ranges, call hierarchy, type hierarchy
- **Relationship to `rescript-editor-analysis.exe`**: The LSP server (Node.js)
  internally calls `rescript-editor-analysis.exe` (native OCaml binary bundled
  with the `rescript` npm package) for analysis. The LSP server is a thin
  wrapper around the analysis binary.

### Build watcher (required)

LSP features like code lens, hover, go-to-definition, and references depend
on `.cmt` files (compiled type info). These must be kept up to date by running
the ReScript compiler in watch mode **in a separate terminal**:

```bash
# ReScript >= 12
npx rescript watch

# ReScript < 12
npx rescript build -w
```

This compiles the project once, then watches for file changes and recompiles
automatically. Without it, LSP features work on stale data.

We intentionally disabled the LSP server's built-in "Start a build?" prompt
(`askToStartBuild: false`) so the user runs the watcher themselves and sees
compiler output directly in their terminal.

How the built-in prompt works internally (for reference): the server spawns
`rescript watch` (or `rescript build -w`) as a background child process via
`utils.runBuildWatcherUsingValidBuildPath()` in `server/src/utils.ts` line ~616.
We may offer this as an integrated terminal action in the future.

### Server configuration

The ReScript LSP server pulls configuration from the client via
`workspace/configuration`. Several features are **disabled by default** and
require the client to send settings to enable them:

| Setting | Default | Our value | Effect |
|---|---|---|---|
| `askToStartBuild` | `true` | `false` | Disabled — user runs watcher in terminal |
| `inlayHints.enable` | `false` | `false` | Inline type annotations (noisy — disabled) |
| `inlayHints.maxLength` | `25` | `25` | Max chars for inlay hint labels |
| `codeLens` | `false` | `true` | Type signatures above declarations |
| `signatureHelp.enabled` | `true` | `true` | Parameter info on function calls |
| `signatureHelp.forConstructorPayloads` | `true` | `true` | Parameter info for variant constructors |
| `incrementalTypechecking.enable` | `true` | (not set) | Only relevant when server runs the watcher |
| `incrementalTypechecking.acrossFiles` | `false` | (not set) | Only relevant when server runs the watcher |

Our plugin sends these settings in two places:
1. **`initializationOptions.extensionConfiguration`** during the `initialize`
   request — the server reads this before any files are opened (critical for
   `askToStartBuild`).
2. **`workspace/configuration` response** when the server pulls config later —
   handled by `ReScriptLanguageClient.createSettings()`.

Source: `rescript-vscode/server/src/config.ts` for all options.
See `ReScriptLanguageServerFactory.kt` for our implementation.

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