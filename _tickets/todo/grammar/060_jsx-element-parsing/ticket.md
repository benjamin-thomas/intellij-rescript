---
summary: Parse JSX into JsxElement/JsxFragment PSI nodes; color names, fold, match braces
created: 2026-07-28
---

# JSX element parsing

## Goal
Replace the token-soup treatment of JSX with real PSI: `JsxElement`,
`JsxFragment`, and `JsxAttribute` nodes built from the structured tokens that
ticket `030` introduced (`JSX_LT`, `JSX_LT_SLASH`, `JSX_SLASH_GT`, `JSX_GT`).
Then hang the visible features off those nodes: tag/attr-name coloring via an
annotator, code folding for multi-line elements, and `<`/`>` brace matching.

## Context
- Ticket `030` deliberately kept tag/attr names as `LIDENT`/`UIDENT` and made
  the lexer contract so this ticket is bnf-first: the four delimiters are
  currently swallowed by `jsxPunctuation` inside `nonDeclToken`
  (`src/main/grammars/ReScript.bnf` ~:297-306) — move them out of there and
  into the new rules, or `Expr` will greedily consume them first.
- Proposed shape (pin on `JSX_LT` / `JSX_LT_SLASH` for recovery):
  `JsxElement ::= JSX_LT jsxName? JsxAttribute* (JSX_SLASH_GT | JSX_GT jsxChild* JsxClosingTag)`
  with `jsxName ::= UIDENT (DOT UIDENT)* | LIDENT`, children = elements,
  fragments, braced blocks, and atomic tokens.
- Folding: `ReScriptFoldingBuilder` anchors on PSI element types — add
  `JSX_ELEMENT`/`JSX_FRAGMENT` with `<tag …>` placeholder text.
- Coloring: an annotator over the nodes colors the name leaves (component
  `UIDENT` vs intrinsic `LIDENT` can differ); consider revisiting
  `JSX_PUNCTUATION`'s KEYWORD fallback at the same time.
- Brace matching: `ReScriptBraceMatcher` pairs — decide whether
  `JSX_LT`/`JSX_GT` pairing is meaningful given `JSX_SLASH_GT`/`JSX_LT_SLASH`
  asymmetry, or whether matching belongs at the element level.
- Existing JSX parser golds (`Jsx*`, `DecoratedDeclaration*`,
  `JsxChildrenElements`) will churn from flat tokens to nested nodes —
  regenerate and review; breadcrumbs for elements may fall out of the PSI via
  the existing breadcrumbs provider (verify).

## Acceptance Criteria
1. `JsxElement`/`JsxFragment`/`JsxAttribute` rules exist in `ReScript.bnf`;
   the four JSX tokens are no longer reachable via `nonDeclToken`.
2. Parser golds show nested element structure with zero error nodes for all
   existing JSX fixtures plus `rescript-playground-example/src/syntax/JsxComponents.res`.
3. Mid-edit recovery: an unclosed `<div` or missing closing tag produces a
   local error inside the element node, not a cascade past the enclosing
   declaration (pin with `assertParserDoesNotCrash`-style fixtures).
4. Tag and attribute names get distinct highlighting via an annotator (unit
   or fixture-tested).
5. Multi-line JSX elements and fragments fold, with sensible placeholder text
   (folding fixture test).
6. All pre-existing non-JSX parser golds unchanged; full suite green.

## Notes
- Visual bar (user feedback, 2026-07-28): current delimiters-only coloring is
  underwhelming next to WebStorm's native JSX and the VS Code ReScript
  plugin — both color tag names (`div`, `section`) and attribute names
  (`className`) distinctly. AC4's annotator is what closes that gap; it is
  the headline payoff of this ticket. (A considered-and-declined stopgap:
  a highlighting-only lexer wrapper remapping LIDENTs lexed in
  JSX_TAG/JSX_CLOSE_TAG states — the user chose to wait for the proper fix.)
- Ticket `030`'s flunk rework landed 2026-07-28 (completed elements now set
  `prevIsExprEnd`), so the lexer contract this ticket builds on is final.
- Ticket `040` (expression parsing) is independent; JSX rules should not wait
  for the Expression hierarchy — they slot into `exprAtom`.
- If scope grows, split: (a) bnf + golds + recovery, (b) annotator/folding/
  brace matching/breadcrumbs.
