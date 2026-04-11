# ReScript IDE — IntelliJ Plugin

ReScript language support for JetBrains IDEs (IntelliJ IDEA, WebStorm, etc.).

## What this is

- **[GrammarKit](https://github.com/JetBrains/Grammar-Kit)** for parsing/PSI
  (syntax highlighting, structure view, folding)
- **[LSP4IJ](https://github.com/redhat-developer/lsp4ij)** for semantic
  intelligence (completion, navigation, diagnostics)
- **Thin glue code** connecting the two

The ReScript LSP (`@rescript/language-server`) handles the semantic work, with
an OCaml-based compiler underneath.

**Under active development.** The plugin currently relies heavily on the
ReScript LSP for semantic features. Over time, the goal is to implement more
features natively — possibly even native type inference, as the intellij-elm
plugin did for Elm. That's probably a long road though, and this is a learning
project as much as a practical tool.

## Plugin evolution strategy — LSP first, then native

For each feature:

1. Enable via LSP (one-line XML)
2. Evaluate — what works? what's missing?
3. Build native PSI replacement when needed
4. IntelliJ automatically prefers native over LSP

The permissive parser lets us start with LSP immediately, then tighten grammar
rules only when a native feature demands it.

| Feature            | LSP                 | Native                                |
|--------------------|---------------------|---------------------------------------|
| Code folding       | (not available)     | DONE                                  |
| Structure view     | DONE (LSP)          | Future (needs name extraction)        |
| Find usages        | LSP references      | Future (PSI resolution)               |
| Rename             | LSP rename          | Future (cross-module)                 |
| Custom inspections | (not available)     | Future (PSI-based)                    |

See [CHANGELOG.md](CHANGELOG.md) for what's shipped and
[`_tickets/todo/`](_tickets/todo/) for what's planned.

## Requirements / How to use

- JetBrains IDE 2025.3+
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) plugin
- `npm install -g @rescript/language-server`
- Set an explicit executable path in `Languages & Frameworks > ReScript`
- The ReScript settings page includes an `Auto-detect` button for common install locations
- `npx rescript watch` running in your project — the language server needs the
  compiler's `.cmt` output to provide diagnostics and completions

## Installation

Search for "ReScript IDE" in your IDE's plugin settings, or install from the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30982-rescript-ide).

## Inspiration and reference material

- [intellij-elm](https://github.com/elm-tooling/intellij-elm) — the gold
  standard; full native implementation with rich editor features. Local copy
  at `~/code/github.com/elm-tooling/intellij-elm`.
- [intellij-rust](https://github.com/intellij-rust/intellij-rust) — everything
  native without LSP (was open-source, now closed-source at JetBrains). Local
  copy at `~/code/github.com/intellij-rust/intellij-rust`.
- [intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp) —
  efficient LSP integration approach; architecture template. Local copy at
  `~/code/github.com/rockofox/intellij-haskell-lsp`.
- [tree-sitter-rescript grammar.js](https://github.com/rescript-lang/tree-sitter-rescript/blob/main/grammar.js) —
  ~140 rules, ~1,100 lines. Reference for building the GrammarKit BNF.
- [ReScript compiler parser (res_core.ml)](https://github.com/rescript-lang/rescript/blob/master/compiler/syntax/src/res_core.ml) —
  grammar rules embedded as comments (search for `::=`).
- [rescript-vscode](https://github.com/rescript-lang/rescript-vscode) — LSP
  server source. Local copy at `~/code/github.com/rescript-lang/rescript-vscode`.
- [Custom Language Support Tutorial](https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html)

## Development

Contributors: see [`_knowledge/INDEX.md`](_knowledge/INDEX.md) for the domain
knowledge base (architecture, lexer, parser, JetBrains APIs) and
[`_knowledge/architecture/DEV_TOOLS.md`](_knowledge/architecture/DEV_TOOLS.md)
for PSI inspection, debugging, and LSP tracing.

### Build and run locally

```bash
./manage/dev/buildPlugin          # build the plugin zip
./gradlew runIde                  # launch a sandboxed IDE with the plugin
./gradlew test                    # run the test suite
./gradlew verifyPluginProjectConfiguration   # catch plugin.xml issues
```

### Publishing

```bash
./manage/prod/release-plugin
```

First release was manual. Plugin ID: `30982`.
See https://plugins.jetbrains.com/plugin/30982-rescript-ide.
