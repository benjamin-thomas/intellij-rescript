---
summary: How language injection works — dual PSI trees, PsiLanguageInjectionHost, StringLiteral vs TemplateLiteral
updated: 2026-04-04
relates: [parser, architecture]
---

## Language injection: dual PSI tree

When a user injects a language (e.g. SQL) into a string via Alt+Enter → "Inject
Language or Reference", IntelliJ does NOT replace the host PSI tree. Instead, it
creates a **second PSI tree** for the injected language alongside the original.
Both coexist — the host tree (ReScript) remains intact, and the injected tree
(SQL) is layered on top in a `VirtualFileWindow`. The injected language's
highlighter takes visual priority, but the host tokens are still there underneath.

## Composite PSI nodes: StringLiteral and TemplateLiteral

Injection requires a single parent PSI node implementing `PsiLanguageInjectionHost`.
Without it, IntelliJ doesn't know which tokens form "the string" to inject into.

We have two separate nodes rather than one generic "string group":

- **`StringLiteral`** — double-quoted `"..."`, single-line, no interpolation.
- **`TemplateLiteral`** — backtick `` `...` ``, multi-line, will have `${expr}` interpolation in the future.

They are separate because interpolation will require different internal structure
(text segments + expression holes). When interpolation is implemented, language
injection should be disabled for template strings that contain `${...}` — the
content is no longer pure text.

## Three pieces for injection to work

1. **`PsiLanguageInjectionHost`** — interface on the PSI node (via mixin in BNF).
   Provides `isValidHost()`, `updateText()`, `createLiteralTextEscaper()`.

2. **`ElementManipulator`** — registered in `plugin.xml`. Tells IntelliJ the
   content range excluding delimiters: `TextRange(1, textLength - 1)` strips the
   quotes/backticks.

3. **`LiteralTextEscaper`** — maps between raw text and decoded text. We use
   `LiteralTextEscaper.createSimple(this)` (platform helper) which does identity
   mapping (no escape processing). This is correct for now but will need refinement
   if we want proper cursor positioning inside injected fragments with escape
   sequences like `\n`.
