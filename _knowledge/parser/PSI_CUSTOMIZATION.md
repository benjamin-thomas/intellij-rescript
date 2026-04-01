---
summary: How to add custom behavior to GrammarKit-generated PSI nodes (mixin pattern, PsiNameIdentifierOwner, navigating children)
updated: 2026-04-01
relates: [jetbrains]
---

# Customizing PSI nodes

GrammarKit generates PSI classes from the BNF and overwrites them on every
`generateParser`. To add custom behavior (getters, setters, interface
contracts), use the **mixin pattern**.

## Inheritance chain

```
ASTWrapperPsiElement          ← IntelliJ base (generic)
    ↑
MyNodeMixin                   ← Our code (custom logic, survives regeneration)
    ↑
MyNodeImpl                    ← Generated (tree accessors, visitor accept)
```

## BNF directives

```bnf
MyNode ::= ... {
    mixin="com.example.psi.impl.MyNodeMixin"       // generated Impl extends this
    implements="com.intellij.psi.SomeInterface"     // generated interface extends this
}
```

- `mixin` — sets the superclass of the generated Impl (instead of ASTWrapperPsiElement)
- `implements` — adds an interface to the generated interface

## PsiNameIdentifierOwner

The key interface for "this PSI node defines a name". Implement it in the
mixin to unlock IDE features:

| Method              | Purpose                                    | Used by                        |
|---------------------|--------------------------------------------|---------------------------------|
| getNameIdentifier() | Returns the PSI element that IS the name   | Find usages, rename, highlight |
| getName()           | Returns the name as a string               | Structure view, go to symbol, breadcrumbs |
| setName(name)       | Replaces the name (for rename refactoring) | Rename (Shift+F6)              |
| getTextOffset()     | Where the caret lands on navigation        | Go to definition               |

Nodes that DON'T implement this interface (Expr, BindingPattern, Decorator,
OpenStatement) are structural — they group tokens but don't introduce a name
into the program.

## Navigating children to find a specific element

Every PSI node has children — the tokens and sub-nodes listed in its `.out`
representation. But the raw tree includes **invisible nodes** (whitespace,
comments) that don't appear in `.out` files.

### Example: finding the name in a LetBinding

For `let x = 1`, the LetBinding's child `BindingPattern` looks like this
in `.out`:

```
ReScriptBindingPatternImpl(BINDING_PATTERN)
  PsiElement(LIDENT)('x')
```

But the **actual** children in memory include whitespace:

```
ReScriptBindingPatternImpl(BINDING_PATTERN)
  PsiWhiteSpace(' ')          ← invisible in .out
  PsiElement(LIDENT)('x')
```

To find the LIDENT, walk children with `firstChild` / `nextSibling` and
**skip trivia** (whitespace, comments). The pattern:

```kotlin
var child = node.firstChild
while (child != null) {
    val type = child.node.elementType
    when (type) {
        TokenType.WHITE_SPACE,
        ReScriptTypes.LINE_COMMENT,
        ReScriptTypes.BLOCK_COMMENT -> { /* skip, continue to nextSibling */ }
        ReScriptTypes.LIDENT -> return child    // found it
        else -> return null                      // something else first — not a simple name
    }
    child = child.nextSibling
}
```

The `else` branch handles destructuring (`LPAREN`, `LBRACE`) and discard
(`UNDERSCORE`) — if the first meaningful token isn't an LIDENT, there's no
single name to extract.

This same walk-and-skip pattern applies whenever you need to find a specific
child element inside a PSI node.

## Current usage

`LetBinding` uses this pattern — see `ReScriptLetBindingMixin.kt`.
The same pattern applies to any named declaration (ModuleBinding,
TypeDeclaration, ExternalDeclaration) when we tighten those rules.
