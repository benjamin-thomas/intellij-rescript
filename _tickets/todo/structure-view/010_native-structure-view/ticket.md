---
summary: Replace the LSP-backed structure view with a native PSI-based one
created: 2026-04-11
---

# Native structure view

## Goal
Implement a native `StructureViewModel` driven by PSI so the structure view
has custom icons, sorting, and filtering — capabilities the LSP
`documentSymbol` response cannot express.

## Context
The structure view is currently provided by LSP4IJ via
`textDocument/documentSymbol`. IntelliJ automatically prefers a native
`StructureViewBuilder` when one is registered for the file type, so this
ticket just needs to add the native side.

Prerequisites: each displayable declaration must implement
`PsiNameIdentifierOwner` (or an equivalent name accessor). Today
`LetBinding`, `ModuleBinding`, and `TypeDeclaration` do — see
`_knowledge/parser/PSI_CUSTOMIZATION.md`. Decorated declarations are wrapped
in `DecoratedDeclaration` (see `_knowledge/architecture/DESIGN_NOTES.md`);
the tree builder should unwrap these so the item shown is the inner
declaration with the decorator rendered as a badge or prefix.

Reference implementations live in local clones of intellij-elm and
intellij-rust (see README).

## Acceptance Criteria
1. `ReScriptStructureViewFactory` returns a native builder for
   `ReScriptFileType`.
2. The tree surfaces top-level `let`, `type`, `module`, `external`, and
   `open` items with their names and icons.
3. Nested modules and their children are walked recursively.
4. Sorting (alphabetical) and visibility filtering toggles are supported
   via the standard `Sorter.ALPHA_SORTER` / `Filter` APIs.
5. Existing LSP structure-view tests are replaced or updated; new tests
   cover the native tree.

## Notes
- This does NOT require tightening `Expr` — only top-level declarations are
  shown.
- `@react.component let make = …` should display as `make` with a
  decorator indicator, not as two items.
