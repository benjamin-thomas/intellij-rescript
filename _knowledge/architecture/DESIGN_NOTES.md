---
summary: Non-obvious design decisions — parser permissiveness, decorator wrapping, folding, semantic tokens, LSP server config
relates: [architecture, parser, lexer, jetbrains]
---

# Design notes

Decisions that aren't obvious from the code, grouped by subsystem.

## Parser: permissive `opaqueBody` strategy

Most declarations (`TypeDeclaration`, `ExternalDeclaration`, `OpenStatement`,
etc.) use `opaqueBody*` as an opaque blob — the parser doesn't understand
their internals. This is intentional. We tighten each rule only when a
concrete feature (breadcrumbs, inspections, completion) demands structured
PSI nodes inside.

**Tightened so far:**

- `LetBinding` — split into `LetBindingPattern EQ Expr`
- `ModuleBinding` — split into `UIDENT EQ ModuleBody`

`Expr` and `ModuleBody` themselves remain opaque (`opaqueBody+`) until a
feature needs their internals.

See `_knowledge/parser/OVERVIEW.md` for the parser's error-recovery strategy
and recommended phases.

## Parser: decorator–declaration association

`@react.component` and the following `let make` are wrapped in a single
`DecoratedDeclaration` PSI node (parent–child). Rule:

```bnf
DecoratedDeclaration ::= Decorator+ bareDeclaration
```

Multiple decorators are flat siblings inside the wrapper (no nesting). Move
Statement, breadcrumbs, and future inspections can treat the decorated unit as
one node.

## Editor: native code folding

The ReScript LSP server does not support `foldingRangeProvider`, so code
folding is native via `ReScriptFoldingBuilder`. Folds multi-line `{ }` blocks.

Folding tests use `<fold text='...'>` markers in `.res` fixture files — these
are IntelliJ test annotations, not ReScript syntax.

## LSP: custom parameter info handler (Ctrl+P)

LSP4IJ's built-in handler truncates the signature label (drops the closing
paren and return type). Our `ReScriptParameterInfoHandler` renders the
complete label.

We don't show documentation in Ctrl+P — users get full docs via Ctrl+Q.

## LSP: semantic tokens (disabled)

LSP4IJ's default semantic token styling underlines variables (inherits from
"Reassigned local variable" in Language Defaults). Disabled via
`ReScriptSemanticTokensFeature`. Can be re-enabled with custom color
mappings.

## LSP: server configuration

| Setting                | Default | Our value | Effect                              |
|------------------------|---------|-----------|-------------------------------------|
| `askToStartBuild`      | `true`  | `false`   | User runs watcher in terminal       |
| `inlayHints.enable`    | `false` | `false`   | Noisy — disabled                    |
| `codeLens`             | `false` | `true`    | Type signatures above declarations  |
| `signatureHelp.enabled`| `true`  | `true`    | Parameter info on function calls    |

Settings are sent via `initializationOptions` (during initialize) and
`workspace/configuration` (when the server pulls later).
See `ReScriptLanguageServerFactory.kt`.
