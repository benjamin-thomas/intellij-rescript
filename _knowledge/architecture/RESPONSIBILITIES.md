# Who rejects what

The recurring question when a form is legal to the plugin but not to `bsc` (or
the reverse) is *which layer should have caught it*. This is the answer, and the
principle that decides the ties.

## The compiler's layers

`bsc` rejects code in two distinct phases, and the boundary is not where it
looks:

| phase | rejects | probe |
| --- | --- | --- |
| **parse** | token stream doesn't form a program | `bsc -only-parse` exits non-zero |
| **type check** | program is well-formed but ill-typed | `-only-parse` exits 0, a full compile fails |

Measured, not assumed:

| input | `-only-parse` | full compile |
| --- | --- | --- |
| `let x = <div></span>` | **error** | error |
| `let x = <A b=foo.Bar />` | **error** | error |
| `let x: int = "a string"` | ok | error |
| `let x = undefinedFunction(1)` | ok | error |

The first two matter: **mismatched JSX tags and uppercase field names are
*syntax* errors, caught at parse time.** They are not type errors. But neither
is expressible in a context-free grammar — a BNF cannot compare two names, nor
condition on the case of an identifier it already matched. The real compiler
enforces them with hand-written checks inside its recursive-descent parser,
which is free to look at anything it likes.

So "syntactic" and "context-free" are not the same set. That gap is where most
of the plugin's apparent divergences live.

## The plugin's layers

| layer | input → output | may reject? |
| --- | --- | --- |
| **lexer** (JFlex) | chars → tokens | **no** |
| **parser** (GrammarKit BNF) | tokens → PSI | only context-free violations, and only as a side effect |
| **annotator / inspection** | PSI → warnings | yes — this is the layer built for it |

### The lexer rejects nothing

Its only job is segmentation: deciding where one token ends and the next
begins. It runs on every keystroke, over code that is invalid for most of its
editing life, and it must never fail — `ReScriptLexerAdapter` catches every
throwable and degrades to `BAD_CHARACTER` rather than let an exception reach the
editor.

The lexer *does* need context (a `<` is a tag or a comparison; a `/` is a regex
or a division), but it consumes that context to *segment*, never to *judge*.
When it cannot tell, it picks the reading that contains the damage — see
`JSX_DECL_RESCUE`, which abandons an unclosed tag at the next declaration-shaped
line rather than swallowing the rest of the file.

### The parser recovers first, rejects second

A `PsiBuilder` grammar's primary product is a usable tree over broken input.
Error reporting is a by-product, and a poor one: GrammarKit reports "expected
X, got Y", which is rarely the actual mistake.

Anything needing name comparison, case rules, or cross-node reasoning belongs in
an inspection, not the BNF. `<div></span>` is the standing example.

## The founding principle: the two failure directions are not symmetric

| direction | example | what the user sees |
| --- | --- | --- |
| **over-accept** — plugin ok, `bsc` errors | `<A b=foo.Bar />` | nothing. `bsc` reports it. |
| **under-accept** — plugin errors, `bsc` ok | `<A b=f(module(M)) />` | **a red squiggle on valid code** |

Over-accepting costs a missed diagnostic the compiler will produce anyway.
Under-accepting tells the user their correct file is wrong, and there is no
second opinion in the editor. **Prefer over-accepting, every time.**

Two consequences that are easy to get backwards:

**Narrowing a rule to match the compiler is not automatically an improvement.**
Tightening `jsxValuePostfix` to the exact ReScript shape (`DOT (UIDENT DOT)*
LIDENT`) reports nothing extra on its own: the attribute value simply ends
early, and the `.`, the `Bar` and the element's own `/>` fall out of the
`JsxElement` as loose siblings — worse PSI on invalid input, no diagnostic
gained. Getting the diagnostic needs the precise suffix *plus* a pinned
uppercase-ending recovery branch; that combination does work (measured: one
error per bad form, every element still owning its `/>`). It is deliberately not
built, because it buys a diagnostic `bsc` already gives, at the cost of a new
recovery rule on the mid-edit path.

The general shape: precision in the grammar is only worth it when it *also*
lands the error inside the right node. Check the gold file, not the rule.

**A deliberate divergence is a decision, not a bug** — but it must be written
down where the rule is. `<div> /a/ </div>` is legal ReScript and the plugin
lexes the `/` as `SLASH` anyway, because a regex there could swallow the `</`
the children state exists to see.

## Applying this

When a divergence turns up, ask in order:

1. **Which direction?** Under-accept → fix it. Over-accept → usually leave it.
2. **Is it context-free?** If catching it needs name comparison or case rules,
   the BNF is the wrong layer regardless of direction — file an inspection.
3. **Does the fix damage recovery?** Check the gold file, not the rule. A
   narrower rule that produces looser PSI is a regression.
4. **Verify against the compiler, by exit status.** See CLAUDE.md — `bsc` prints
   a blank line before a syntax error, so sampling its output reports errors as
   legal.
