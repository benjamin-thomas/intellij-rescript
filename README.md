# ReScript IDE — IntelliJ Plugin

ReScript language support for JetBrains IDEs.

Built with [GrammarKit](https://github.com/JetBrains/Grammar-Kit) for native
syntax support and [LSP4IJ](https://github.com/redhat-developer/lsp4ij) for
semantic features.

**Under active development.** The plugin currently relies heavily on the
ReScript LSP for semantic features (completion, diagnostics, go-to-definition).
Over time, the goal is to implement more features natively — possibly even
native type inference, as the intellij-elm plugin did for Elm. That's probably
a long road though, and this is a learning project as much as a practical tool.

Inspiration:
- [intellij-elm](https://github.com/elm-tooling/intellij-elm) — the gold standard;
  full native implementation with rich editor features
- [intellij-rust](https://github.com/intellij-rust/intellij-rust) — everything
  native without LSP (was open-source, now closed-source at JetBrains)
- [intellij-haskell-lsp](https://github.com/rockofox/intellij-haskell-lsp) —
  efficient LSP integration approach

See [CHANGELOG.md](CHANGELOG.md) for what's shipped and [GOAL.md](GOAL.md) for
what's planned.

## Requirements / How to use

- JetBrains IDE 2025.3+
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) plugin
- `npm install -g @rescript/language-server`
- Set an explicit executable path in `Languages & Frameworks > ReScript`
- The ReScript settings page includes an `Auto-detect` button for common install locations
- `npx rescript watch` running in your project (the language server needs the
  compiler's output to provide diagnostics and completions)

## Installation

Search for "ReScript IDE" in your IDE's plugin settings, or install from the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30982-rescript-ide).
