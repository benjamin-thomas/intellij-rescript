---
summary: Move Statement Up/Down on a JSX child line moves the whole enclosing declaration
status: todo
form: sketch
eta: half a day
created: 2026-09-26
---

# Move Statement inside JSX children

## Goal

With the caret on a child line of a multi-line JSX element, Move Statement
Up/Down (Ctrl+Shift+Up/Down) swaps the whole component `let` with the
neighboring declaration. When the component is the only item in a module, it
does nothing. It should move the JSX child among its siblings.

## Context

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptStatementMover.kt`
  is a `LineMover` subclass. `findMovableAncestor` climbs to the nearest
  declaration-level node (`LET_BINDING`, `DECORATED_DECLARATION`, …).
- JSX elements are not movable units, so a child line resolves to the enclosing
  binding. When that binding has no movable sibling (e.g. it is the first item
  in a `module X = {`), `toMove2 = null` blocks the move entirely.
- PSI: the children of `JsxElement` / `JsxFragment` are `JsxElement`,
  `JsxFragment`, braced blocks and loose tokens (see `jsxChild` in
  `src/main/grammars/ReScript.bnf`).
- Tests: `ReScriptStatementMoverTest`, fixture-based
  (`performEditorAction(ACTION_MOVE_STATEMENT_UP_ACTION)`).

## Reproduce

```rescript
let a = 1

let make = () =>
  <div>
    <p> {React.string("1")} </p>
    <p> {React.string("2")}<caret> </p>
  </div>
```

- Move Statement Up in the plugin: the whole `let make` binding moves above
  `let a = 1`.
- Expected: the two `<p>` lines swap. Both the file and the swapped result exit
  0 under `bsc -only-parse`.
- The same shape as the only item in `module A = { @react.component let make = … }`:
  nothing moves.

## Acceptance Criteria

1. With the caret on a child element (single- or multi-line), the move carries
   that child past the previous or next sibling child. The enclosing
   declaration stays put.
2. A move never carries the enclosing declaration along. At the first or last
   child the child does not leave its parent's tags.
3. With the caret on a declaration line, behavior is unchanged; all existing
   mover tests stay green.

## Notes

- Braced children (`{…}`) count as siblings too. How loose tokens (`hello`,
  `"text"`) are handled is the worker's call.
- Out of scope: moving attributes within a multi-line opening tag.
- Depends on: none.
- Touches only the mover and its tests, so it is safe to run in parallel with
  every other jsx ticket.
