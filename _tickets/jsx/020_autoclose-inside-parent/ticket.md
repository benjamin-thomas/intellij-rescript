---
summary: Typing `>` for a new child tag inside an existing element or fragment inserts no closing tag
status: todo
form: sketch
eta: half a day
created: 2026-09-26
---

# Auto-close a tag typed inside an existing element

## Goal

Auto-close (typing `>` after `<div` inserts `</div>`) only works at the root of
a JSX tree. Most new tags are typed inside an existing element or fragment, and
there it silently does nothing, because the new element takes the parent's
closing tag as its own.

## Context

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptJsxTypedHandler.kt`:
  the pure query `jsxAutoCloseText` and its helpers `elementCloseText` /
  `fragmentCloseText` decide "already closed" structurally: the element has a
  `jsxClosingTag`, or the fragment has a `</`.
- The grammar cannot correlate tag names (see the `JsxMismatchedClosingTag`
  fixture). For `<div>` ⏎ `<span>` ⏎ `</div>`, the parser gives `<span>` the
  `</div>` as its closing tag and leaves `<div>` unclosed. So the new element
  looks closed even though the tag belongs to an ancestor. The same happens
  when a tag nests inside one with the same name, and inside fragments.
- CLAUDE.md puts name comparison outside the grammar. This handler is exactly
  such a place.
- Tests: `ReScriptJsxAutoCloseTest` (pure query, PSI built from text) and
  `ReScriptJsxTypedHandlerTest` (the real typing pipeline).

## Reproduce

Type `>` at `<caret>` (`myFixture.type('>')`):

```rescript
let x =
  <div>
    <span<caret>
  </div>
```

- Plugin: the result is `<span>` and nothing is inserted.
- Expected: `<span></span>`. The resulting file is valid (`bsc -only-parse`
  exit 0).

The same thing happens (nothing inserted) for:

- `<div<caret>` inside `<div>…</div>`;
- `<Row<caret>` inside `<Row>…</Row>`;
- `<li<caret>` before an existing `<li>…</li>` sibling inside `<ul>`;
- `<<caret>` and `<p<caret>` inside `<>…</>`;
- a multi-line opening tag whose `>` is typed on its own line inside a parent.

Auto-close still works today at the top level, inside `{…}` children, and
inside a render prop's `{…}`.

## Acceptance Criteria

1. Each case under Reproduce inserts the matching closing tag (`</span>`,
   `</div>`, `</Row>`, `</li>`, `</>`, `</p>`), with the caret between the tags.
2. No duplicates: retyping the opening `>` of an element that really has its own
   closing tag still inserts nothing. That includes same-name nesting where both
   levels are closed (`<div><div|></div></div>`).
3. The existing auto-close tests stay green, and the insertion is still one undo
   step.

## Notes

- Known, out of scope: a type parameter list that starts a line (`array` ⏎
  `<int`) lexes as JSX (pinned by `JsxStatementPositionTypeClash`), so typing
  its `>` inserts `</int>`.
- Out of scope: completing `/>`, highlighting tag pairs, keeping renamed tags in
  sync (the follow-ups listed when the first auto-close ticket closed).
- Depends on: none.
- Touches only the typed handler and its two test classes; no other jsx ticket
  does, so it is safe to run in parallel with all of them.
- jsx/030 changes how some *unclosed* states lex, but not the PSI shape this
  handler reads (an element followed by an ancestor's closing tag).
