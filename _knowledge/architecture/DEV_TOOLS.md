---
summary: PSI inspection, plugin debugging, and end-to-end LSP tracing for this plugin
relates: [architecture, parser, jetbrains]
---

# Developer tools

## Inspecting the PSI tree

1. **PsiViewer plugin** (recommended) — install from the JetBrains marketplace:
   https://plugins.jetbrains.com/plugin/227-psiviewer

2. **Built-in PSI Viewer** — requires enabling IntelliJ's internal mode:
   Help > Edit Custom Properties, add `idea.is.internal=true`, restart.
   Then: Tools > View PSI Structure.

## Debugging the plugin

- **IDE log**: `tail -f build/idea-sandbox/IU-2025.3/log/idea.log`
- **LSP traces**: In `runIde`, Settings > Languages & Frameworks >
  Language Servers > select ReScript > Debug tab > set trace to verbose.
  Then View > Tool Windows > Language Servers > LSP Consoles > Traces tab.
- **Debugger**: In the Gradle tool window, find `runIde` under
  Tasks > intellij platform > runIde, right-click > Debug.

## Tracing an LSP feature end-to-end

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

### Testing the analysis binary directly

Useful for debugging without a running IDE. Positions are 0-indexed (LSP
convention).

```bash
cd ~/code/github.com/benjamin-thomas/7guis/rescript-7guis

# Make sure the project is compiled first (needs .cmt files)
npx rescript build

# Then query directly
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe definition src/App.res 77 15
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe hover src/App.res 77 15 true
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe references src/App.res 33 4
./node_modules/@rescript/linux-x64/bin/rescript-editor-analysis.exe documentSymbol src/App.res
```

Run the binary with no args to see all available commands.

### Testing the LSP server directly

Demonstrates the full protocol (initialize → initialized → didOpen →
definition over stdio):

```bash
./manage/dev/test-lsp src/App.res 77 15
```

The server is stateful — it needs initialization and file open before queries,
and it needs time to process files after `didOpen`.

## Key files in the LSP server codebase (`rescript-vscode` repo)

| File                              | Role                                                |
|-----------------------------------|-----------------------------------------------------|
| `server/src/server.ts`            | JSON-RPC router — dispatches LSP methods to handlers |
| `server/src/utils.ts`             | `runAnalysisCommand()` — shells out to the OCaml binary |
| `analysis/bin/main.ml`            | CLI entry point — parses argv, calls Commands       |
| `analysis/src/Commands.ml`        | Feature implementations (definition, hover, etc.)   |
| `analysis/src/References.ml`      | Symbol resolution for definition/references         |
| `analysis/src/SignatureHelp.ml`   | Signature help implementation                       |
| `analysis/src/DocumentSymbol.ml`  | Document symbols (used by structure view)           |

## Build watcher (required for LSP features)

LSP features depend on `.cmt` files. Run the compiler in watch mode:

```bash
npx rescript watch    # v12+
npx rescript build -w # older
```

We disabled `askToStartBuild` so the user runs the watcher themselves.
