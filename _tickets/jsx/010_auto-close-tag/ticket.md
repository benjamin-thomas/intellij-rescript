---
summary: Typing `>` after a JSX opening tag auto-inserts the matching closing tag
status: doing
form: sketch
created: 2026-09-16
---

# Auto-close JSX tag

## Goal
When `>` is typed where it completes a JSX opening tag, insert the matching
`</TagName>` after the caret with the caret between the two. This is the
single most-used JSX editing behavior in every JSX-aware IDE; the plugin
currently has no typed handler at all.

## Context
The parser already produces `JsxElement` / `JsxTagName` / `JsxClosingTag`
nodes and the lexer has a full JSX state machine, so no grammar work is
needed. The pattern to follow is `ReScriptQuoteHandler` (registered as
`lang.quoteHandler` in `plugin.xml`); the IntelliJ extension point for
typing behavior is `TypedHandlerDelegate` (`com.intellij.codeInsight.editorActions.TypedHandlerDelegate`),
registered as `typedHandler` in `plugin.xml`.

Look at `src/test/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptQuoteHandlerTest.kt`
for how a typing-side behavior is tested in this codebase.

## Acceptance Criteria
1. Typing `>` where it completes a JSX opening tag inserts `</TagName>`
   after the caret; the caret lands between the tags (before the inserted
   text).
2. Fragments work: typing `>` after `<>` inserts `</>`.
3. A `>` that does not complete a JSX tag must not trigger: comparisons
   (`a > b`), arrows (`=>`), anything outside JSX.
4. No duplicate: if a matching closing tag already follows the caret,
   typing `>` does not insert another one.
5. Tag names with paths work: `<Mod.sub>` → `</Mod.sub>`.
6. The insertion is one undo step.
7. Tests follow the existing fixture/typing-test pattern; all existing
   tests keep passing.

## Notes
- 2026-09-16: branch logic extracted into pure query `jsxAutoCloseText(leaf,
  textAfterCaret): String?` in `ReScriptJsxTypedHandler.kt`; branch matrix is
  characterized in `ReScriptJsxAutoCloseTest` (PSI built via PsiFileFactory, no
  editor), while `ReScriptJsxTypedHandlerTest` stays as the end-to-end wiring
  test (delegate registered, insertion + caret placement).
- Out of scope, follow-up tickets if wanted: completing `/>` when typing
  `/` at the end of an opening tag; tag-pair highlight (`<div>` ↔
  `</div>`); rename-tag sync; tag-name completion (LSP territory).
- Hyphenated tag names: check `parser/fixtures/JsxHyphenatedTagNames.res`
  for whether the grammar admits them before assuming they must work.
- Decision from the conversation (2026-09-16): chosen over grammar/040
  because the JSX parsing layer is the most complete part of the grammar
  (v0.7.0/v0.7.1 were all JSX); the gaps are editing-side, and this needs
  no expression-parsing prerequisite.
