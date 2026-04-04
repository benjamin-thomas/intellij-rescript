# Changelog

## v0.4.0 — Language Injection

### Editor features
- **Language injection**: inject SQL, HTML, RegExp, or any other language into string literals via Alt+Enter → "Inject Language or Reference". Works on both `"double-quoted"` and `` `backtick` `` strings.
- **Inspection suppression**: `// noinspection` comments above declarations now suppress warnings (e.g. from injected languages)

## v0.3.0 — Navigation & Smarter Parsing

### Navigation
- **Breadcrumbs**: see where you are in the file (`module Foo > let make`) at the bottom of the editor
- **Move Statement** (`Alt+Shift+Up/Down`): move entire declarations up/down, including decorated ones (`@react.component let make` moves as a unit)

### Syntax
- Decorators (`@react.component`, `@module("fs")`, `@as(...)`) are now properly associated with their declaration
- `.resi` interface files: let signatures and module signatures parse correctly
- Nested block comments
- Full numeric literal support (hex, octal, binary, underscores, scientific notation, BigInt)
- ReScript v12 operators (`&&&`, `|||`, `^^^`, `**`, `===`, `!==`, `:>`, `..`, etc.)

## v0.2.1 — Fixes & Polish

### LSP
- "Server not found" notification is now scoped per project (previously only the first project was notified in multi-project sessions)
- Improved notification message with install command and PATH guidance
- Parameter info handler no longer blocks a shared thread pool; UI updates are now dispatched on the EDT

### Internal
- Extracted shared string token sets to avoid divergence between parser and highlighter
- Removed dead test code in brace matcher tests
- Plugin verifier now runs locally via `./gradlew verifyPlugin`
- Build script dynamically picks up the version number

## v0.2.0 — Editor Smarts

### Editing
- Commenter: toggle line comments with `Ctrl+/` and block comments with `Ctrl+Shift+/`
- Brace matcher: auto-close `{}`, `()`, `[]` and highlight matching pairs
- Quote handler: auto-close `"` and `` ` `` strings with step-over support

### Lexer
- Reworked string tokenization into distinct states (`STRING_START` / `STRING_CONTENT` / `STRING_ESCAPE` / `STRING_END`) and template states (`TEMPLATE_START` / `TEMPLATE_CONTENT` / `TEMPLATE_END`), enabling escape sequence highlighting

### Navigation
- "Go to Related" (`Ctrl+Alt+Home`): jump between `.res` and `.resi` counterpart files

### Project View
- File nesting: `.res.js` and `.res.mjs` output files are nested under their source `.res` file

### File Creation
- "New > ReScript File" action with Module (`.res`) and Interface (`.resi`) templates

### Language Support
- Spell checking in comments and string literals
- New keywords: `async`, `await`, `try`, `catch`, `while`, `for`, `and`, `as`

## v0.1.0 — Initial release

### File type support
- Recognize `.res` and `.resi` files as ReScript source and interface files, with dedicated file icons

### Lexer
- Syntax highlighting for all core token categories: keywords, identifiers, literals, strings, operators, delimiters, decorators, comments
- Regex literal support (`/pattern/flags`) with previous-token disambiguation

### Parser / PSI
- Permissive GrammarKit parser with typed PSI nodes for all major top-level declarations: `let`, `type`, `module`, `open`, `include`, `external`, `exception`, decorators, and extension points
- Recursive brace nesting and error recovery

### Editor features
- Native code folding for multi-line `{ }` blocks

### LSP integration
- Automatic detection of `@rescript/language-server` on PATH
- Full LSP feature set via LSP4IJ: hover, completion, go-to-definition, references, rename, diagnostics, formatting, code actions
- Structure view via LSP document symbols
- Custom parameter info handler (`Ctrl+P`) — fixes LSP4IJ's truncated signature labels
- Quick documentation (`Ctrl+Q`) with markdown rendering
