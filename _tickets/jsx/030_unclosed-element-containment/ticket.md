---
summary: An unclosed or half-typed JSX element leaks children-mode lexing into later declarations
status: todo
form: sketch
eta: 1 day
created: 2026-09-26
---

# Contain an unclosed JSX element

## Goal

While a tag is being typed, or after a closing tag goes missing, valid
declarations further down the file get red squiggles. The lexer stays in
JSX-children mode past the broken element. CLAUDE.md makes the lexer
responsible for containing damage from half-typed code; do that for JSX
children, the way the opening-tag state already does.

## Context

`src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex`:

- `JSX_CHILDREN` has no escape hatch. Once an element loses its closing tag,
  every later `<` glued to a name is `JSX_LT` (so `array<int>` and `x<y` open
  phantom elements) and `/` is always `SLASH`. This lasts until some later tag
  happens to balance the child count. A `{` switches back to expression mode,
  which is why damage inside a `module X = {…}` body is contained by accident.
- `JSX_TAG` already has a "declaration-shaped next line" rescue
  (`JSX_DECL_RESCUE`, `dropAbandonedChildrenFrame`), and `JSX_CLOSE_TAG` has a
  guarded newline bail. Those are the precedent.
- `JSX_TAG` has no rule for `</`. While an opening tag is still being typed
  (`<sp` ⏎ `</div>`), the parent's closing tag lexes as `BAD_CHARACTER`s, and
  its `>` then opens a *new* children region.
- The frame stack and child-count machinery are the subtle part. The packed
  restart state and `checkCorrectRestart` must keep holding.
- Parser recovery fixtures pin today's behavior: `JsxMissingClosingTag`,
  `JsxUnterminatedClosingTag`, `JsxTagNewlineRescue`,
  `JsxUnterminatedBracedChild`, plus the lexer fixtures `JsxTagNewlineRescue*`.

## Reproduce

All three inputs are broken, and `bsc -only-parse` exits 2 on each, as it
should. The trailing declarations are valid on their own (bsc exit 0 when
parsed alone).

A missing closing tag:

```rescript
let a =
  <div>
    <span>
  </div>

let b: array<int> = []
let c = (x, y) => x<y
```

Plugin: besides the expected error at the unclosed element, there are errors on
`let b` (`<int>` lexed as a JSX tag) and on `let c` (`x<y` lexed as a tag).

The state right after typing `<sp` inside a parent:

```rescript
let a =
  <div>
    <sp
  </div>

let b: array<int> = []
let c = (x, y) => x<y
```

Plugin: the same leak onto `let b` and `let c`.

A half-typed closing tag:

```rescript
module A = {
  @react.component
  let make = () =>
    <div>
      <span> {React.string("x")} </
    </div>
}

let b: array<int> = []
```

Plugin: the expected error at `</`, plus an error inside `let b` at `<int>`.

## Acceptance Criteria

1. In each reproduction, every error sits inside the broken element. `let b` and
   `let c` lex exactly as they do on their own: comparison and type-argument
   tokens, no JSX tokens.
2. A missing closing tag followed by a new top-level declaration does not affect
   how that declaration lexes. The legal shapes the existing rescues protect
   still work: `module(M)` children, closing tags split across lines, comments
   in tags.
3. When an opening tag is interrupted by `</name>`, that closing tag closes the
   enclosing element instead of opening a new children region.
4. All existing JSX lexer and parser fixtures pass. Recovery golds may change,
   but each change is reviewed and justified. Restart tests stay green.

## Notes

- `@` cannot start a JSX child: `<div>` ⏎ `@foo x` ⏎ `</div>` gives bsc exit 2.
  So the `JSX_DECL_RESCUE` shapes are safe to reuse in children.
- jsx/020 makes the missing-closing-tag state common: today, every tag the user
  types inside a parent is left unclosed.
- Containment is the goal, not bsc-exact diagnostics (the over-accept
  principle).
- Mismatched closing names (`<div></span>`) are an annotator concern and out of
  scope.
- Depends on: none.
- Overlaps in `ReScript.flex`: the `JSX_TAG` block with jsx/040 (value rules)
  and jsx/050 (identifier rules). Schedule it apart from them, or expect a hand
  merge in that block.
- Recovery golds also overlap with grammar/070, which rewrites error-message
  wording in the same fixtures.
