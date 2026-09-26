---
summary: Lex `\"…"` escaped identifiers — JSX attribute names like `\"aria-label"`, and everywhere else
status: todo
form: sketch
eta: 2h
created: 2026-09-26
---

# Escaped identifiers

## Goal

ReScript writes an identifier that is a keyword, or not a valid plain name, as
`\"…"` (`\"type"`, `\"aria-label"`, `\"MyWeirdProp"`). The lexer has no rule
for this. Outside strings, `\` is `BAD_CHARACTER` in every state, so every use
gets a red squiggle. JSX attribute names are where users hit it most visibly.

## Context

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex`:
  identifiers are `LOWER_IDENT` / `UPPER_IDENT`, plus `JSX_HYPHEN_IDENT` in the
  tag states. No state has a rule for `\"`: the catch-all turns `\` into
  `BAD_CHARACTER`, and the rest lexes as a string literal.
- Positions seen in real code:
  - JSX attribute names: `<div \"aria-label"="x" />`,
    `<MyWeirdComponent \"MyWeirdProp"="bar" />`;
  - labelled parameters: `~\"aria-label": string=?`, `~\"import"=true`;
  - record type fields and field access: `{\"MyWeirdProp": string}`,
    `props.\"MyWeirdProp"`;
  - let names and patterns: `let \"type" = 1`, `Some(\"export")`;
  - operator definitions and uses: `\"||||"(a, b)`, `\"@"(a, b)`.
- The parser accepts `LIDENT` in all these positions, so if the new token *is*
  an identifier token, no grammar change should be needed. Confirm this when
  picking up the ticket.
- `ReScriptJsxAnnotator` colors the attribute-name `LIDENT`.

## Reproduce

Every snippet exits 0 under `bsc -only-parse`.

```rescript
let el = <MyWeirdComponent \"MyWeirdProp"="bar" \"aria-label"="x" />
```

Plugin: `\` becomes `BAD_CHARACTER`, with the error "<jsx attribute>, DOT,
JSX_GT, JSX_SLASH_GT or LBRACE expected, got '\'".

```rescript
let \"type" = 1
let x = \"type"
type props = {\"MyWeirdProp": string}
```

Plugin: one error on each line.

## Acceptance Criteria

1. `\"…"` lexes as one identifier token in expression context, in JSX children,
   and inside opening tags (both attribute names and unbraced values). The
   snippets above parse with no error element.
2. In an opening tag, the escaped name is the `JsxAttribute` name and gets the
   attribute-name color.
3. Strings are unaffected: `"a\"b"` still lexes as string content with an
   escape. Restart tests stay green.

## Notes

- Reusing `LIDENT` is simplest; a dedicated token is also an option, and the
  choice is the worker's. `\"Foo"` is a legal `let` name (bsc exit 0), so don't
  derive the case from the first letter without checking bsc.
- Escaped tag names are legal too (`<\"my-el"> </\"my-el">`, bsc exit 0).
  Include them if they fall out for free; otherwise they are out of scope.
- Not JSX-only; it could move under `grammar`. It is filed here because JSX
  attribute names are where users meet it.
- Depends on: none.
- Overlaps in `ReScript.flex`: the shared block and the `JSX_TAG` block with
  jsx/030 and jsx/040, and the macro section with jsx/010.
