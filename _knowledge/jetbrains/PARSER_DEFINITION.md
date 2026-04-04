---
summary: getStringLiteralElements and getCommentTokens — what they're actually used for
updated: 2026-04-04
relates: [lexer, parser]
---

# ParserDefinition string/comment methods

## getStringLiteralElements()

Tells the platform which tokens are string literals.

**Used by**: spell checking (check spelling inside strings) and "Find in String
Literals" search filter. Note: this does NOT enable language injection — injection
requires composite PSI nodes implementing `PsiLanguageInjectionHost` plus
registered `ElementManipulator`s (see `_knowledge/jetbrains/LANGUAGE_INJECTION.md`).

**Not related to**: syntax highlighting (that's `SyntaxHighlighter`) or quote
auto-pairing (that's `QuoteHandler`). These three features happen to need
similar token lists but are completely independent systems.

**Important**: this alone does NOT enable spell checking — a
`SpellcheckingStrategy` is also required. See SPELLCHECKER.md.

## getCommentTokens()

Tells the platform which tokens are comments.

**Used by**: the platform for brace matching (comments are ignored when
scanning for matching braces), code folding, and other structural operations.

**Not sufficient for spell checking** — same caveat as above.
