# JetBrains

- **QUOTE_HANDLER.md** — SimpleTokenSetQuoteHandler: position-based opening/closing, hasNonClosedLiteral override
- **BRACE_MATCHER.md** — PairedBraceMatcher: auto-close suppression, structural pairs, highlight shifting
- **PARSER_DEFINITION.md** — getStringLiteralElements, getCommentTokens: what they're actually used for
- **GOTO_RELATED.md** — GotoRelatedProvider: file-level navigation without custom keybindings
- **SPELLCHECKER.md** — SpellcheckingStrategy: why it's required, bundledModule ID pitfall, what didn't work
- **LANGUAGE_INJECTION.md** — Dual PSI trees, PsiLanguageInjectionHost, StringLiteral vs TemplateLiteral design
- **INSPECTION_SUPPRESSOR.md** — The `// noinspection` comment mechanism: 4-step flow, what we test
- **ANNOTATOR.md** — Semantic highlighting the SyntaxHighlighter cannot do; the PsiElement.children gotcha; designing PSI so the annotator needs no heuristics
- **TEST_FRAMEWORK_LEAKS.md** — ParsingTestCase's mock app poisoning JVM-wide extension caches for later BasePlatformTestCase runs; why to call `parseFile` rather than `createPsiFile`
