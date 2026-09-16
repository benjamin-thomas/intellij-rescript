---
summary: Typing `>` after a JSX opening tag auto-inserts the matching closing tag
status: done
form: spec
created: 2026-09-16
---

# Auto-close JSX tag

## Goal
When `>` is typed where it completes a JSX opening tag, insert the matching
`</TagName>` after the caret with the caret between the two. This is the
single most-used JSX editing behavior in every JSX-aware IDE; the plugin
had no typed handler at all.

## Context
Files changed:

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptJsxTypedHandler.kt`
  (new) — `TypedHandlerDelegate` registered as `<typedHandler>`; on
  `charTyped('>')` it commits the document, resolves the leaf the typed char
  became, asks the pure query `jsxAutoCloseText(gt)`, and inserts the result
  + moves the caret inside the typing command's write action (→ one undo
  step). The decision logic lives in `jsxAutoCloseText` with
  `elementCloseText` / `fragmentCloseText` helpers.
- `src/main/resources/META-INF/plugin.xml` — `<typedHandler>` registration.
- `src/test/kotlin/.../lang/ReScriptJsxAutoCloseTest.kt` (new) — query-level
  characterization: PSI built via `PsiFileFactory`, last char of the input
  plays the just-typed `>` (`|` marks it when it is not the last char). No
  editor machinery, one case per test.
- `src/test/kotlin/.../lang/ReScriptJsxTypedHandlerTest.kt` (new) — wiring
  tests through the real typing pipeline: insertion + caret placement, and
  the single-undo guarantee.

Key non-obvious detail: inside `charTyped` the just-typed char is not in the
PSI tree until `PsiDocumentManager.commitDocument()` is called — without it
`findElementAt(caret - 1)` returns null.

## Acceptance Criteria
1. Typing `>` where it completes a JSX opening tag inserts `</TagName>`
   after the caret; the caret lands between the tags. — wired test
   `testGreaterThanClosesTag`.
2. Fragments: `>` after `<>` inserts `</>`; the `>` of an existing `</>`
   inserts nothing. — `testInsertsClosingTagForFragment`,
   `testNoInsertWhenTypedGtEndsFragment`.
3. A `>` that does not complete a JSX tag never triggers — anything lexing
   as something other than `JSX_GT` (comparison, `=>`), a closing tag's `>`,
   a `>` outside JSX. — `testNoInsertForComparisonGt`,
   `testNoInsertWhenTypedGtEndsClosingTag`, plus the `JSX_GT` token guard.
4. No duplicate closing tag: retyping the opening `>` of an element or
   fragment that already has its closing tag inserts nothing — decided
   structurally (`jsxClosingTag != null` / fragment `JSX_LT_SLASH` present),
   not textually. — `testNoInsertWhenElementAlreadyHasClosingTag`.
5. Module-path tags: `<Mod.sub>` → `</Mod.sub>`. —
   `testInsertsClosingTagForModulePathTag`.
6. The insertion is one undo step. — wired test `testAutoCloseIsOneUndoStep`.
7. Full suite green — 24 classes / 334 tests at completion.

## Steps
1. Added `ReScriptJsxTypedHandler` (commit-document + leaf resolution +
   insert/caret shell) and registered it in `plugin.xml`.
2. Added `ReScriptJsxTypedHandlerTest` (end-to-end wiring tests).
3. Extracted the decision into pure query `jsxAutoCloseText(gt)` and
   characterized its branch matrix in `ReScriptJsxAutoCloseTest`.
4. Replaced the text-prefix duplicate check with structural checks — fixes
   duplicate insertion when retyping the opening `>` of a closed element.
5. Added fragment support (`<>` → `</>`).
6. Characterized module-path tags; ran the full suite.

## Notes
- 2026-09-16 decision: this was picked over `grammar/040` (expression
  parsing) because the JSX parsing layer is the plugin's most complete part
  (v0.7.0/v0.7.1 were all JSX); the gaps were editing-side, and this needs
  no expression-parsing prerequisite.
- Out of scope, follow-up tickets if wanted: completing `/>` when typing
  `/` at the end of an opening tag; tag-pair highlight (`<div>` ↔
  `</div>`); rename-tag sync; tag-name completion (LSP territory).
