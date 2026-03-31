---
summary: PairedBraceMatcher — auto-close suppression, structural pairs, highlight shifting
updated: 2026-03-31
relates: [lexer]
---

# PairedBraceMatcher

Highlights matching `{}()[]` and auto-inserts closing braces.

## Three methods

- **`getPairs()`**: returns `BracePair` array. The `structural` boolean marks
  whether the pair is a code block boundary (used for folding). Only `{}` is
  structural in ReScript.

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
