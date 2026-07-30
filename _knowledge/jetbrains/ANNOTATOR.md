# Annotator — semantic highlighting on top of the lexer

## When you need one

The `SyntaxHighlighter` sees token types only. Any color that depends on a token's ROLE
rather than its type needs an `Annotator`, because only the PSI knows the role.

JSX is the worked example: a tag name and a variable are both `LIDENT`. No lexer change can
distinguish them without breaking the lexer/parser separation.

## The pattern

```kotlin
holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
    .range(leaf)
    .textAttributes(key)
    .create()
```

Registered as `<annotator language="ReScript" implementationClass="..."/>` in `plugin.xml`.
Annotation attributes layer over the syntax-highlighter pass.

## Gotcha: `PsiElement.children` omits token leaves

It returns composite children only. To reach the tokens inside a node, go through the AST:

```kotlin
node.getChildren(TokenSet.create(LIDENT, UIDENT))
```

Non-recursive, and it skips unwanted token types by construction.

## Design the PSI so the annotator needs no heuristics

The JSX annotator is ~40 lines with zero guards, because the grammar was shaped for it:

- one node type per role (`JsxTagName`, `JsxAttribute`, `JsxSpreadAttribute`)
- an attribute's NAME is its only direct `LIDENT` child; values live inside a nested
  `JsxAttributeValue`, so a value can never be miscolored as a name
- opening and closing tags reuse the same `JsxTagName` node, so they color identically
  for free
- a hyphenated name lexes as ONE token, so it colors as one unit

If an annotator needs a guard, the PSI shape is usually the thing to fix.

## Choosing fallback keys

`MARKUP_TAG` / `MARKUP_ATTRIBUTE` render as plain text in common dark themes, and the XML
plugin's HTML keys are not on our classpath. `KEYWORD`, `CLASS_NAME` and `INSTANCE_FIELD`
are styled by every bundled theme.
