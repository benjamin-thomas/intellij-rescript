# intellij-rescript — ReScript Plugin for JetBrains IDEs

## What this is

A ReScript plugin for JetBrains IDEs (IntelliJ, WebStorm, etc.).

- **GrammarKit** for parsing/PSI (syntax highlighting, structure, folding)
- **LSP4IJ** for semantic intelligence (completion, navigation, diagnostics)
- **Thin glue code** connecting the two

The ReScript LSP (`@rescript/language-server`) handles semantic work
(OCaml-based compiler underneath).

## Reference material

- [intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp) —
  Architecture template. Local copy at
  `~/code/github.com/rockofox/intellij-haskell-lsp`.
- [tree-sitter-rescript grammar.js](https://github.com/rescript-lang/tree-sitter-rescript/blob/main/grammar.js) —
  ~140 rules, ~1,100 lines. Reference for building the GrammarKit BNF.
- [ReScript compiler parser (res_core.ml)](https://github.com/rescript-lang/rescript/blob/master/compiler/syntax/src/res_core.ml) —
  Grammar rules embedded as comments (search for `::=`).
- [intellij-elm](https://github.com/elm-tooling/intellij-elm) —
  Mature IntelliJ language plugin. Local copy at
  `~/code/github.com/elm-tooling/intellij-elm`.
- [intellij-rust](https://github.com/intellij-rust/intellij-rust) —
  Archived, good reference. Local copy at
  `~/code/github.com/intellij-rust/intellij-rust`.
- [Custom Language Support Tutorial](https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html)
- [GrammarKit](https://github.com/JetBrains/Grammar-Kit) — BNF syntax reference
- [LSP4IJ](https://github.com/redhat-developer/lsp4ij) — the LSP client framework we use
- [rescript-vscode](https://github.com/rescript-lang/rescript-vscode) — LSP server source.
  Local copy at `~/code/github.com/rescript-lang/rescript-vscode`.

## Developer tools

### Inspecting the PSI tree

1. **PsiViewer plugin** (recommended) — install from the JetBrains marketplace:
   https://plugins.jetbrains.com/plugin/227-psiviewer

2. **Built-in PSI Viewer** — requires enabling IntelliJ's internal mode:
   Help > Edit Custom Properties, add `idea.is.internal=true`, restart.
   Then: Tools > View PSI Structure.

### Debugging the plugin

- **IDE log**: `tail -f build/idea-sandbox/IU-2025.3/log/idea.log`
- **LSP traces**: In `runIde`, Settings > Languages & Frameworks >
  Language Servers > select ReScript > Debug tab > set trace to verbose.
  Then View > Tool Windows > Language Servers > LSP Consoles > Traces tab.
- **Debugger**: In the Gradle tool window, find `runIde` under
  Tasks > intellij platform > runIde, right-click > Debug.

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

## Design notes

### Custom parameter info handler (Ctrl+P)

LSP4IJ's built-in handler truncates the signature label (drops closing paren
and return type). Our `ReScriptParameterInfoHandler` renders the complete label.
We don't show documentation in Ctrl+P — users get full docs via Ctrl+Q.

### Code folding (native, PSI-based)

The ReScript LSP server does not support `foldingRangeProvider`, so code folding
is native via `ReScriptFoldingBuilder`. Folds multi-line `{ }` blocks.

Folding tests use `<fold text='...'>` markers in `.res` fixture files — these
are IntelliJ test annotations, not ReScript syntax.

### Decorator-declaration association

`@react.component` and the following `let make` are **siblings** in the PSI
tree. To make them parent-child (for refactoring/inspections), we'd add:
`DecoratedDeclaration ::= Decorator+ block_declaration`.

### LSP semantic tokens (disabled)

LSP4IJ's default semantic token styling underlines variables (inherits from
"Reassigned local variable" in Language Defaults). Disabled via
`ReScriptSemanticTokensFeature`. Can be re-enabled with custom color mappings.

### Build watcher (required)

LSP features depend on `.cmt` files. Run the compiler in watch mode:

```bash
npx rescript watch    # v12+
npx rescript build -w # older
```

We disabled `askToStartBuild` so the user runs the watcher themselves.

### Server configuration

| Setting | Default | Our value | Effect |
|---|---|---|---|
| `askToStartBuild` | `true` | `false` | User runs watcher in terminal |
| `inlayHints.enable` | `false` | `false` | Noisy — disabled |
| `codeLens` | `false` | `true` | Type signatures above declarations |
| `signatureHelp.enabled` | `true` | `true` | Parameter info on function calls |

Settings sent via `initializationOptions` (during initialize) and
`workspace/configuration` (when server pulls later).
See `ReScriptLanguageServerFactory.kt`.

### Installing the plugin locally

```bash
./manage/dev/buildPlugin
```

### Publishing to the JetBrains Marketplace

```bash
./manage/prod/release-plugin
```

First release was manual. Plugin ID: `30982`.
https://plugins.jetbrains.com/plugin/30982-rescript-ide

## Plugin evolution strategy — LSP first, then native

For each feature:
1. Enable via LSP (one-line XML)
2. Evaluate — what works? what's missing?
3. Build native PSI replacement when needed
4. IntelliJ automatically prefers native over LSP

The permissive parser lets us start with LSP immediately, then tighten
grammar rules only when a native feature demands it.

| Feature | LSP | Native |
|---|---|---|
| Code folding | (not available) | DONE |
| Structure view | DONE (LSP) | Future (needs name extraction) |
| Find usages | LSP references | Future (PSI resolution) |
| Rename | LSP rename | Future (cross-module) |
| Custom inspections | (not available) | Future (PSI-based) |

## Next steps

### Next up (ordered by effort, smallest first)

1. ~~**Full numeric literals**~~ — DONE. Hex, octal, binary, underscores, scientific
   notation, BigInt suffix (`n`). Hex floats skipped (undocumented, niche).
2. ~~**Nested block comments**~~ — DONE. Depth-tracking lexer state + folding support.
3. ~~**v12 operators**~~ — DONE. `&&&`, `|||`, `^^^`, `~~~` (bitwise), `<<`, `>>`, `>>>`
   (shifts), `**` (exponentiation), `===`, `!==` (strict equality), `:>` (coercion),
   `..` (range).
4. ~~**Tighten LetBinding**~~ — DONE. Split into `LET REC? BindingPattern EQ Expr`.
   `BindingPattern` and `Expr` are public PSI nodes. `LetBinding` implements
   `PsiNameIdentifierOwner` via mixin — `getName()` extracts the LIDENT from
   the binding pattern (returns null for destructuring/discard).
5. **Move Statement Up/Down** (Alt+Shift+Up/Down) — move entire top-level
   declarations as a unit. Annotations move with their declaration
   (`@react.component` + `let make` move together). Uses `StatementUpDownMover`.
   No grammar changes needed — purely structural PSI navigation (~130 lines).
6. **Tighten ModuleBinding + rename BindingPattern** — Three sub-tasks:
   a. Rename `BindingPattern` → `LetBindingPattern` (it's specific to let bindings).
   b. Tighten `ModuleBinding` from `MODULE body_token*` to
      `MODULE UIDENT EQ ModuleBody` with mixin + `PsiNameIdentifierOwner`.
      `ModuleBody` is a public opaque PSI node (like `Expr` but for modules).
   c. Same pattern for `TypeDeclaration` if time permits.
7. **Breadcrumbs** — show the path in the PSI tree at the bottom of the editor
   (e.g. `Foo.res > module App > let make`). Uses `getName()` from #4 and #6.
   Implement a `BreadcrumbsProvider` (~28 lines).
8. **StringLiteral PSI node** — wrap `STRING_START STRING_CONTENT* STRING_END` in a
   composite node. Required for language injection (Alt+Enter → "Inject Language or
   Reference" for SQL, etc.)
9. **Regex internals highlighting** — break `/pattern/flags` into sub-tokens.
10. **Native structure view** — custom icons, sorting, filtering. Depends on #4.
11. **Go to Test / Implementation** (Ctrl+Shift+T) — switch between `src/Foo.res` and
    `test/Foo_test.res` (or similar naming convention). Uses `GotoTestOrCodeHandler`.
    Phase 1: file-level navigation based on naming convention.
    Phase 2: function-level focus — jump to the test/implementation of the function
    under the cursor (à la Cursive for Clojure). Depends on #4.
    Phase 3: if the test function doesn't exist, offer to create it with an empty body
    (e.g. `let test_make = () => { }` in the test file).
12. **Template string interpolation** — `${expr}` inside backtick strings. Lexer state
    switches back to `YYINITIAL` inside `${}` with brace depth tracking.
13. **JSX token awareness** — lexer states for `<div>`, `<Component />`. Needs
    disambiguation: `<` after an identifier is comparison, otherwise JSX (same
    pattern as regex/division).

### Longer term

14. **Expression parsing** — tighten `body_token*` into real expression rules.
15. **Extract variable / function** — select an expression, create a `let name = <expr>`.
    Depends on #14 for validating that the selection is a complete expression.
16. **Decorator-declaration wrapping** — parent-child instead of siblings.
