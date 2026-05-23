---
summary: Add a general ReScript inspection that reports native parser errors
created: 2026-05-23
---

# ReScript Parser Error Inspection

## Goal
Add a native IntelliJ inspection for ReScript files so parser errors are visible
through IntelliJ inspection flows, instead of only being noticed when a file is
opened in the editor.

The first version should be a general ReScript inspection that reports
`PsiErrorElement` nodes produced by the native GrammarKit parser. This creates a
foundation for future ReScript checks such as unused variables or other
PSI-based diagnostics.

## Context
The plugin currently registers a parser definition, syntax highlighter, LSP
integration, and an `InspectionSuppressor`, but it does not register a
`LocalInspectionTool`.

Relevant files:
- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptParserDefinition.kt`
- `src/main/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptInspectionSuppressor.kt`
- `src/test/kotlin/com/github/benjamin_thomas/intellij_rescript/lang/ReScriptInspectionSuppressorTest.kt`

Current behavior: parser errors show when a file is opened and highlighted, but
there is no ReScript inspection that can be run over a project/scope to reveal
parser errors across unopened files.

The inspection should be named/identified generally, for example
`ReScriptInspection`, so the initial parser-error check can later grow into
broader native ReScript checks if that remains desirable.

This ticket is about native parser errors only. It should not attempt to surface
LSP/compiler diagnostics, and it should not build a custom background
project-wide scanner.

## Acceptance Criteria
1. A ReScript `LocalInspectionTool` is registered in `plugin.xml` with a stable inspection ID such as `ReScriptInspection`, enabled for ReScript files.
2. The inspection reports every `PsiErrorElement` in `.res` and `.resi` files as an inspection problem with a useful message and source location.
3. Running IntelliJ inspections over a project/scope can surface parser errors in ReScript files without manually opening each file first.
4. `// noinspection ReScriptInspection` suppression works with the existing `ReScriptInspectionSuppressor` behavior.
5. Tests cover at least one valid ReScript file with no reported inspection problems and one invalid ReScript file where a parser error is reported.

## Notes
- Keep this scoped to inspection integration and parser-error reporting.
- Do not implement unused variable checks or semantic checks in this ticket.
- Do not implement custom Problems tool window plumbing in this ticket.
- If future checks need independent severity or suppression, split them into separate inspections later.
