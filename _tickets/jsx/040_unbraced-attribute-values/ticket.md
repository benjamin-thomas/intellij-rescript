---
summary: Accept the unbraced attribute values bsc allows — `list{}`, `dict{}`, regex, tagged template — and lex `true`/`false` as keywords in tags
status: todo
form: sketch
eta: half a day
created: 2026-09-26
---

# Remaining unbraced attribute values

## Goal

Close the remaining gap between the unbraced attribute values bsc accepts and
those `JsxAttributeValue` can express, so formatter-stable code stops showing
red squiggles. `b=list{1, 2}` and `b=/re/` both come out of `bsc -format`
unchanged.

## Context

- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScript.flex`,
  `JSX_TAG` state. There is no rule for a glued `list{` or `dict{`, for a `/`
  that starts a regex, or for a template glued to an identifier. There are no
  keyword rules either, so `true` and `false` arrive as `LIDENT` and are
  highlighted as identifiers, unlike everywhere else.
- `src/main/grammars/ReScript.bnf`: `JsxAttributeValue`, `jsxValueAtom`,
  `jsxValuePostfix`, and the comments above them explaining which value shapes
  are admitted and why.
- The `JsxAttributeValueApplication` fixture records `list{…}` as a deliberate
  gap: "a braced suffix on a path is indistinguishable from `<A b=x {...p} />`
  without whitespace sensitivity". But bsc *is* whitespace-sensitive there:
  `<A b=list{1, 2} />` is a list value, while `<A b=list {...p} />` is the value
  `list` followed by a spread. Both exit 0, so the lexer can tell them apart by
  the glued `{`.
- The `JsxIdentAttributeValues` fixture pins `true` as `LIDENT`.

## Reproduce

Every snippet below exits 0 under `bsc -only-parse`.

- `let el = <A b=list{1, 2} />`: the plugin reports "… expected, got '{'" plus
  an error at `/>`. `bsc -format` keeps it unbraced.
- `let el = <A b=/re/ />`: the plugin reports "<jsx attribute value> or
  QUESTION expected, got '/'". `bsc -format` keeps it.
- `let el = <A b=dict{"k": 1} />`: same errors as `list{`. `bsc -format` adds
  braces.
- ``let el = <a href=j`https://x/$id` />``: the plugin reports "… expected,
  got '`'". `bsc -format` adds braces. This shows up in older code, e.g. the
  wildcards-world-ui idempotency tests in the rescript repo.
- `let el = <button disabled=true hidden=false />`: no error, but both values
  are `LIDENT` and get the identifier color.

## Acceptance Criteria

1. The four value shapes above parse with no error element, and their tokens
   stay inside the `JsxAttributeValue`.
2. `<A b=list {...p} />` (space before `{`) still parses as the value `list`
   followed by a `JsxSpreadAttribute`.
3. `b=true` / `b=false` lex as `TRUE` / `FALSE` inside an opening tag.
   Identifiers that merely start with a keyword (`trueValue=`) stay `LIDENT`.
4. The `JsxAttributeValueApplication` and `JsxIdentAttributeValues` golds are
   updated (the `list{}` line stops erroring). `JsxInvalidNegativeAttributeValue`
   still produces no value node. Restart tests stay green.

## Notes

- Out of scope: a JSX element as an unbraced value (`<A b=<C /> />`). bsc
  accepts it (exit 0) and the formatter adds braces. Returning from the inner
  element to the outer opening tag needs a new lexer frame kind, and the packed
  2-bit kind field is full, so it would need a layout change. That is too big
  to ride along; it deserves its own ticket only if someone asks for it.
- Keywords used as attribute *names* stay `LIDENT`. bsc rejects
  `<A open=… />` anyway, and over-accepting is fine.
- Depends on: none.
- Overlaps in `ReScript.flex`: the `JSX_TAG` block with jsx/030 and jsx/050. A
  hand merge is likely if they run at the same time.
- Overlaps in `ReScript.bnf` with jsx/060, but in different rules (value atoms
  vs. block content), so the risk is low.
