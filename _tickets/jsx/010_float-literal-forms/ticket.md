---
summary: Float literals `8.`, `1e3`, `1.e2` lex as INT plus DOT/LIDENT, breaking unbraced JSX attribute values
status: todo
form: sketch
eta: 2h
created: 2026-09-26
---

# Float literals with a trailing dot or a bare exponent

## Goal

Lex every float spelling bsc accepts as one `FLOAT` token. Today `8.` is
`INT` + `DOT` and `1e3` is `INT` + `LIDENT`. Inside an unbraced JSX attribute
value that stray `DOT` becomes a field-access postfix that swallows the next
attribute, so formatter-stable code like `<Popover offset=8. />` shows red
squiggles. Of everything this survey turned up, this causes the most parse
errors in real ReScript React code.

## Context

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex`:
  the `FLOAT` macro requires digits after the dot and has no
  exponent-without-dot form. The shared `<YYINITIAL, JSX_CHILDREN>` block and
  the `JSX_TAG` state both use it.
- `src/main/grammars/ReScript.bnf`: `JsxAttributeValue` / `jsxValuePostfix`.
  An `INT` followed by `DOT` and an identifier is a legal field postfix, so the
  damage lands on the *next* attribute. Before a `>` or `/>` the element
  instead ends silently, and the only error shows up at the distant closing
  tag, or nowhere.
- Outside JSX the mis-lexing only affects colors (the opaque expression rules
  accept `INT DOT`). The visible symptom is `1e3` highlighted half as a number
  and half as an identifier.
- Fixtures: `lexer/fixtures/NumericFloats.res` (+ `.out`); `parser/fixtures/Jsx*`.
- Real-world evidence: the rescript-shadcn registry (`sideOffset=8.`,
  `spacing=2.>`, `offset=10.` on its own line before `>`), Base UI / React Aria
  bindings (`value=42.`), covid-19charts (`strokeWidth=2.`). A corpus of 2,846
  bsc-valid JSX files under `~/code` had 74 files with plugin parse errors.
  Widening `FLOAT` alone cleared 24 of those 74, including 17 of the 20
  failing rescript-shadcn files.

## Reproduce

```rescript
let el = <Popover offset=8. crossOffset={-4.} />
```

- `bsc -only-parse`: exit 0. `bsc -format` keeps `offset=8.` unbraced.
- Plugin: `8` INT, `.` DOT. `. crossOffset` is taken as a field postfix of the
  value, then error "<jsx attribute>, DOT, JSX_GT, … expected, got '='" at
  crossOffset's `=`, plus a second error at `/>`.

```rescript
let el =
  <ToggleGroup spacing=2.>
    <ToggleGroup.Item id="top" />
  </ToggleGroup>
```

- bsc: exit 0.
- Plugin: the element silently ends after `2`. The `.` and the `>` fall out of
  it, and the only error is reported at `</ToggleGroup>`.

```rescript
let el = <A b=1e3 d=1.e2 e=1_000. />
```

- bsc: exit 0.
- Plugin: no error element, but the PSI is wrong. `e3` becomes a punned
  attribute, `d`'s value is `1` plus field `.e2`, and the element's `/>` falls
  outside the `JsxElement`.

## Acceptance Criteria

1. `8.`, `1_000.`, `1e3`, `1E-3`, `1.e2`, `2.5e-3` each lex as a single `FLOAT`
   in expression context, in JSX children, and inside an opening tag.
2. The three snippets above parse with no error element, and every attribute
   (and the element's own `/>` or `>`) stays inside its `JsxElement`.
3. Dotted forms that are not floats keep today's tokens (`x.y`, `...`, `..`,
   `+.`, `*.`, `1.->Float.toString`). Check each against bsc before encoding it.
4. Restart tests (`checkCorrectRestart`) stay green, and `NumericFloats` covers
   the new forms.

## Notes

- bsc lexes `1.` greedily: `let r = 1..2` is a syntax error (exit 2). Nothing
  to match there, but don't regress `DOTDOT` elsewhere.
- Out of scope: negative literals in a tag. `b=-1.` stays unlexable by design;
  see `JsxInvalidNegativeAttributeValue`.
- Not JSX-only: the bug is lexer-wide, and JSX is simply where it turns into
  errors. It could move under `grammar`.
- Depends on: none.
- Shares `ReScript.flex` with jsx/030, jsx/040 and jsx/050, but only the macro
  definitions above the rules, not the state blocks. jsx/050 adds an identifier
  macro nearby, so expect at most an adjacent-line conflict with it.
- Like every lexer/parser ticket, it appends to `ReScriptLexerTest` /
  `ReScriptParserTest`, which gives trivial append conflicts.
