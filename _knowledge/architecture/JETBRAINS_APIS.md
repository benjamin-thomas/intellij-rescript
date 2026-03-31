---
summary: Non-obvious behaviors of JetBrains platform APIs we consume
updated: 2026-03-31
relates: [lexer]
---

# JetBrains APIs — Non-obvious behaviors

This documents JetBrains platform APIs whose behavior isn't self-explanatory.
Reading the Javadoc alone doesn't always make the actual mechanics clear.

## SimpleTokenSetQuoteHandler

**What it does**: handles auto-closing quotes (type `"`, get `""` with cursor
between) and step-over (type `"` at a closing quote, cursor moves past it).

**The non-obvious part**: it does NOT distinguish opening tokens from closing
tokens. All tokens passed to the constructor are stored in a flat `TokenSet`.
The handler decides opening vs closing purely by **cursor position within the
token**:

- `isOpeningQuote`: is the token in the set AND `offset == iterator.start`?
- `isClosingQuote`: is the token in the set AND `offset == iterator.end - 1`?
- `isInsideLiteral`: is the token in the set? (any position)

This means `STRING_START` works as an "opener" not because of its name, but
because it's a single-character token and the cursor is always at position 0
(the start) after typing `"`.

**`hasNonClosedLiteral` override**: the default implementation scans line tokens
to detect unclosed strings, but it assumes single-token strings. With our
multi-token design (`STRING_START` + `STRING_CONTENT` + `STRING_END`), the
default gets confused and auto-pairs inside unclosed strings. We override it to
check: is the cursor on a `STRING_START`/`TEMPLATE_START`? If yes, auto-pair
(it's a fresh quote). If no, don't (we're inside an existing string).

## PairedBraceMatcher

**What it does**: highlight matching `{}()[]` and auto-insert closing braces.

Three methods to implement:

- **`getPairs()`**: returns `BracePair` array. The `structural` boolean on
  `BracePair` marks whether the pair is a code block boundary (used for folding).
  Only `{}`  is structural in ReScript.
- **`isPairedBracesAllowedBeforeType(lbraceType, contextType)`**: called when
  the user types an opening brace. `contextType` is the token immediately after
  the cursor. Returning `true` means "always auto-insert the closing brace."
  Returning `false` suppresses auto-close (e.g. Rust suppresses before
  identifiers to avoid inserting `}` when wrapping existing code). We return
  `true` always.
- **`getCodeConstructStart(file, openingBraceOffset)`**: when the cursor is on
  a closing brace, IntelliJ highlights the matching opener. This method can
  shift the highlight start earlier (e.g. to include the `if` keyword before
  a `{`). We return `openingBraceOffset` unchanged.

## ParserDefinition.getStringLiteralElements()

**What it does**: tells the platform which tokens are string literals.

**Used by**: spell checking (check spelling inside strings), "Find in String
Literals" search filter, and language injection (Alt+Enter → "Inject Language").

**Not related to**: syntax highlighting (that's `SyntaxHighlighter`) or quote
auto-pairing (that's `QuoteHandler`). These three features happen to need
similar token lists but are completely independent systems.

## GotoRelatedProvider

**What it does**: provides items for the "Go to Related Symbol" action
(Ctrl+Alt+Home or platform-dependent shortcut).

**How it works**: IntelliJ calls `getItems(psiElement)` with the current file.
We return the counterpart file (`.res` ↔ `.resi`). The platform handles the
keybinding — we don't define shortcuts, so it works regardless of the user's
keymap.
