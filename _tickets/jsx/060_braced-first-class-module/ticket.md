---
summary: `module(M)` inside braces — a `{module(M)}` JSX attribute or child, record fields, record patterns — is parsed as a module declaration
status: todo
form: sketch
eta: 2h
created: 2026-09-26
---

# First-class module inside braces

## Goal

A first-class module value parses fine unbraced (`b=module(M)`, a `module(Env)`
child), but not inside `{…}`. There, `module` is taken as the start of a
`module X = …` declaration. So `{module(M)}` gets a red squiggle wherever it
appears: as a JSX attribute value, a JSX child, a record field, or inside a
record pattern.

## Context

- `src/main/grammars/ReScript.bnf`: `bracedBlock` → `blockContent` tries
  `blockDeclaration` first. `ModuleBinding` is pinned on `MODULE`, so it commits
  as soon as it sees `module` and then fails on `(`.
- `firstClassModuleExpr` (`MODULE parenBlock`, pinned after the paren) already
  exists; unbraced positions use it. `ModuleTypeDeclaration` is the other rule
  that starts with `MODULE` and must keep working.
- Existing golds: `LetFirstClassModule*`, `JsxFirstClassModuleChild`,
  `TypeFirstClassModule*`.
- Real-world examples:
  - rescript-shadcn `SidebarDemo.res`: records
    `{name, logo: module(Icons.Logo), plan}` and patterns
    `Some({logo: module(Logo), name, plan})`;
  - rescript-shadcn `src/generated/DemoLoader.res`: object fields
    `"AccordionBasic": module({…})`, 834 errors in one file;
  - rescript's own printer test `jsx.res`: `pack={module(Foo)}` and a
    `{module(Foo: Bar)}` child.

## Reproduce

Each line exits 0 under `bsc -only-parse`. The plugin reports
"TYPE or UIDENT expected, got '('" at the `(` on each:

```rescript
let el = <Sidebar logo={module(Icons.Logo)} />
let el2 = <div> {module(Foo)} </div>
let teams = [{name: "Acme", logo: module(Icons.Logo)}]
```

## Acceptance Criteria

1. These parse with no error element:
   - the three lines above;
   - `{module(Foo: Bar)}`;
   - the record pattern `| Some({logo: module(Logo), name}) => …`.
2. Inside braces, `module X = {…}`, `module type T = {…}` and decorated module
   bindings still parse as declarations; the existing nested-module golds are
   unchanged.
3. `let module(M) = …`, and `module(M)` in unbraced positions, are unchanged.

## Notes

- Not JSX-only: the record and pattern cases are the more common ones. It could
  move under `grammar`.
- Out of scope: `module type X = module type of M` also fails today, but that is
  a separate grammar gap.
- Depends on: none.
- Overlaps in `ReScript.bnf` with jsx/040, in different rules (block content vs.
  value atoms).
- Also overlaps with grammar/040 (rewriting the opaque rules into expressions).
  Don't run the two at the same time.
