# JSX — grammar shape and the decisions behind it

The rules are small; the reasoning is not obvious from them.

## The rules

```bnf
private exprAtom     ::= jsxValue | firstClassModuleExpr | delimitedBlock | nonDeclToken
private parenContent ::= jsxValue | delimitedBlock | declKeyword | AT | PCT_PCT | nonDeclToken
private jsxValue     ::= JsxFragment | JsxElement

JsxFragment ::= JSX_LT JSX_GT jsxChild* JSX_LT_SLASH JSX_GT { pin = 2 }
JsxElement  ::= JSX_LT JsxTagName (JsxAttribute | JsxSpreadAttribute)*
                (JSX_SLASH_GT | jsxOpenTail) { pin = 1 }
private jsxOpenTail ::= JSX_GT jsxChild* JsxClosingTag { pin = 1 }
JsxClosingTag ::= JSX_LT_SLASH JsxTagName JSX_GT { pin = 1 }

JsxTagName        ::= UIDENT (DOT UIDENT)* (DOT LIDENT)? | LIDENT
JsxAttribute      ::= QUESTION LIDENT | LIDENT (EQ QUESTION? JsxAttributeValue)?
JsxAttributeValue ::= literal | bracedBlock | jsxVariantChild | jsxPathChild
JsxSpreadAttribute ::= LBRACE DOTDOTDOT blockContent* RBRACE

private jsxChild ::= jsxValue | firstClassModuleExpr | delimitedBlock | literal | comment
                   | jsxPathChild | jsxVariantChild | jsxExtensionChild | TRUE | FALSE
private jsxPathChild ::= UIDENT (DOT UIDENT)* (DOT LIDENT)? | LIDENT (DOT LIDENT)*
```

## Why each non-obvious choice

**`jsxChild` is an explicit whitelist, never `nonDeclToken`.** `nonDeclToken` used to reach
`JSX_LT_SLASH`; a child matching that swallows its own element's closing tag and every
paired element rolls back to soup. Declaration keywords are excluded so an unclosed element
cannot absorb the next `let`. Operators are excluded because `<div>a + b</div>` is a syntax
error — children are space-separated atoms, not expressions.

**Four pins, not three.** Without `jsxOpenTail`, the tail alternative fails atomically when
a closing tag is missing: it un-consumes `>` and all children, and the error escapes to
`Expr` level with a nonsense message. Extracting the tail into its own pinned rule is what
keeps the error inside the element.

**`JsxFragment` pins at 2, never 1.** Both start with `JSX_LT`. Fragment-first dispatch only
works because the fragment is unpinned at token 1 and rolls back cleanly on `<div…`.

**`JsxTagName` and `jsxPathChild` differ deliberately.** As a tag name, `<div.foo />` is
invalid — an uppercase module path may end in at most one lowercase segment. As a child,
`foo.bar.baz` is field access and legal.

**Ordering is load-bearing.** `jsxValue` must precede `nonDeclToken` in both `exprAtom` and
`parenContent`.

## Deliberately not done

- **`<`/`>` brace matching.** `BracePair` maps one left type to one right. `JSX_LT` would
  need both `JSX_GT` and `JSX_SLASH_GT`; `JSX_GT` closes both `JSX_LT` and `JSX_LT_SLASH`;
  `<div/>` has no `JSX_GT`. Matching-tag highlighting is a different, PSI-level API.
- **Mismatched tag names** (`<div></span>`) parse clean. BNF cannot compare names; this
  needs a semantic inspection.

## Traps

`structureItemRecover` needs `JSX_LT` in its negative set. Once the soup fallback was
deleted there was nothing to absorb stray delimiters, and recovery ran past its boundary.

GrammarKit suppresses a deferred error when a failed alternative progressed past the error
point (`exit_section_impl_`). Some invalid inputs therefore produce only a file-level error,
not a local one. Fighting this means fighting the generator runtime.
