---
summary: SpellcheckingStrategy — why it's required, bundledModule ID pitfall, what didn't work
updated: 2026-03-31
relates: [lexer]
---

# Spell Checking

## Key insight: it's NOT automatic

IntelliJ does NOT spell-check custom languages automatically.
`ParserDefinition.getCommentTokens()` and `getStringLiteralElements()` are used
for other purposes (brace matching, find-in-strings, etc.) — they do NOT wire
up spellchecking.

A `SpellcheckingStrategy` registered via `<spellchecker.support>` in plugin.xml
is required.

## Minimum implementation

```kotlin
class ReScriptSpellcheckingStrategy : SpellcheckingStrategy() {

    override fun isMyContext(element: PsiElement): Boolean =
        ReScriptLanguage.`is`(element.language)

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        return when (element.node.elementType) {
            ReScriptTypes.STRING_CONTENT, ReScriptTypes.TEMPLATE_CONTENT -> TEXT_TOKENIZER
            else -> super.getTokenizer(element)
        }
    }
}
```

`isMyContext` is essential — without it, the strategy is never called for
ReScript files.

The base class `SpellcheckingStrategy` already handles `PsiComment` elements,
so comments don't need explicit handling in `getTokenizer`.

## Gradle dependency

The spellchecker is a **bundled module** (not a bundled plugin):

```kotlin
intellijPlatform {
    bundledModule("intellij.spellchecker")  // NOT bundledPlugin("com.intellij.spellchecker")
}
```

The ID is `intellij.spellchecker` — the `com.intellij.` prefix does NOT work.

## plugin.xml registration

Register directly in the main plugin.xml — no need for optional dependency:

```xml
<spellchecker.support
        language="ReScript"
        implementationClass="...ReScriptSpellcheckingStrategy"/>
```

The spellchecker module is always present in IntelliJ — it cannot be disabled
by users, so an optional `<depends>` is unnecessary.

## What didn't work

- **Optional dependency**: `<depends optional="true" config-file="rescript-spellchecker.xml">`
  silently failed in runIde. The strategy was never loaded.
- **Wrong Gradle ID (bundledPlugin)**: `bundledPlugin("com.intellij.spellchecker")` —
  Gradle can't resolve it, it's not a plugin.
- **Wrong Gradle ID (prefix)**: `bundledModule("com.intellij.spellchecker")` —
  wrong prefix, needs `intellij.` not `com.intellij.`.
- **Integration tests**: `enableInspections(SpellCheckingInspection::class.java)`
  throws "Unregistered inspections requested" — the inspection isn't available
  in the test sandbox even with the bundledModule dependency.
- **Missing `isMyContext`**: without it, the strategy compiles and registers but
  is never invoked for ReScript files.

## Reference

The Rust plugin (`intellij-rust`) implements `RsSpellcheckingStrategy` with the
same pattern. The Elm plugin does not implement spell checking.
