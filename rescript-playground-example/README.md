# ReScript Playground

A compact, hand-curated ReScript project used as a **manual smoke-test
fixture** for the IntelliJ ReScript plugin. It is intentionally small and
topic-focused so each file exercises one slice of plugin behavior — syntax
highlighting, structure view, folding, breadcrumbs, decorator association,
file nesting, LSP startup, and the planned implementation/test toggle.

This is **not** a production app and **not** unit-test data for the parser.
Parser fixtures live under
`src/test/resources/com/github/benjamin_thomas/intellij_rescript/`.

## Open in IntelliJ

1. Open the IDE with the plugin loaded:
   ```bash
   manage/dev/runIde
   ```
   (Run from the plugin repo root.)
2. The sandbox IDE will open this directory as a project.

## Compile

The project depends only on `rescript@^12.2.0`. ReScript 12 ships its
standard library (`Console`, `Int`, `Array`, `Math`, `Promise`, …) without
needing a separate `@rescript/core` dependency.

```bash
cd rescript-playground-example
npm install
npx rescript build
# or, while the LSP is running:
npx rescript build -w
```

`npm install` and `node_modules/` are intentionally **not** committed.

## What each folder exercises

| Path                      | Plugin behavior under test                                                                                              |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `src/syntax/Records.res`  | Record types, `@as("…")` field attributes, optional fields, `private` records, record-update spread.                    |
| `src/syntax/Variants.res` | Regular variants with inline records, polymorphic variants (`#tag`), or-patterns, exhaustive switch.                    |
| `src/syntax/Strings.res`  | Double-quoted strings, escapes, template literals, template interpolation (v0.5.0), language-injection hosts (v0.4.0).  |
| `src/syntax/Numbers.res`  | Hex/octal/binary/underscored numeric literals, BigInt, scientific notation, v12 operators (`&&&`, `\|\|\|`, `^^^`, `**`, `===`, `!==`). |
| `src/syntax/Modules.res`  | Module signatures, nested modules, module aliases — structure view tree, breadcrumb depth.                              |
| `src/syntax/PatternMatching.res` | Tuple / record / list destructuring, switch with guards, exception throwing.                                     |
| `src/syntax/Externals.res`| `@val`, `@module`, `@scope`, `@new`, `@send` decorated externals — the parser's decorator association path.            |
| `src/syntax/AsyncAwait.res` | `async`/`await` keywords (v0.2.0), `try`/`catch` around `await`, pipe (`->`) into promise chains.                     |
| `src/syntax/Decorators.res` | Stacked decorators on a binding — exercises `Move Statement` (v0.3.0) keeping decorator + decl together.              |
| `src/syntax/MutualRecursion.res` | Mutually recursive `type … and …` and `let rec … and …`, plus a decorated `and` continuation — each member gets its own structure node. |
| `src/navigation/UserCard.res` ↔ `tests/navigation/UserCardTest.res` | File-level Go-to-Test (planned: `src/Foo.res` ↔ `tests/FooTest.res`). |
| `src/navigation/OrderService.res` ↔ `tests/navigation/OrderServiceTest.res` | Function-level Go-to-Test target: `test_<name>` pairs with `<name>` (planned). |
| `src/app/Main.res`        | Top-level expression statements (v0.4.1), file nesting of `.res.mjs` output.                                            |

## Test-file convention

Pairs mirror the **planned** plugin convention from
`_tickets/todo/navigation/010_go-to-test-file-level/`:

- `src/<sub>/Foo.res` ↔ `tests/<sub>/FooTest.res` (sibling `tests/` directory,
  CamelCase `Test` suffix).
- Function-level: `let foo` in the source maps to `let test_foo` in the test
  (per `_tickets/todo/navigation/020_*`).

Once the `testFinder` is wired up, `Ctrl+Shift+T` on any file or function
here should jump cleanly.

## Things to manually verify in the IDE

1. **Syntax highlighting** — open each file in `src/syntax/`; keywords,
   strings, decorators, numeric literals should be colorized distinctly.
2. **Structure view** (`Alt+7`) — top-level declarations should be visible,
   nested modules should be hierarchical.
3. **Breadcrumbs** (bottom of editor) — inside a nested module, you should
   see `module Math > module Vec2 > let add`.
4. **Folding** — `{` / `}` blocks fold; multi-line `/* … */` comments fold.
5. **File nesting** (Project view, gear icon → File Nesting must include
   `.res` parents nesting `.res.mjs` children) — after `npx rescript build`,
   each `.res.mjs` should nest under its `.res`.
6. **Comment toggle** — `Ctrl+/` line-comments, `Ctrl+Shift+/` block-comments.
7. **Move Statement** (`Alt+Shift+Up/Down`) on `Decorators.res` — the
   `@deprecated` line moves with its binding.
8. **Language injection** — in `Strings.res`, `Alt+Enter` on `sqlSnippet`
   should offer "Inject Language or Reference"; choose SQL and confirm
   highlighting.
9. **LSP** — with `npx rescript build -w` running and
   `Languages & Frameworks > ReScript` configured, you should get hover,
   completion, diagnostics, and Go-to-Definition.
10. **Error recovery** — temporarily corrupt a file (e.g., delete an `=` in
    a `let`) and confirm the parser keeps highlighting the surrounding
    declarations rather than red-squiggling the whole file.

## Known plugin gaps

A note here should describe valid ReScript that the plugin mishandles, not
ReScript errors.

- **Move Statement on consecutive top-level expression statements** (CHANGELOG v0.4.1).
  `src/app/Main.res` has two adjacent `Console.log(…)` lines; `Alt+Shift+Up/Down`
  moves them as a single block, not individually. Documented limitation of the
  opaque-parsing strategy until expressions get real grammatical structure.
