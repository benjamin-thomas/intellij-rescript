---
summary: The three roles of the apostrophe in ReScript — char literal, type variable, identifier tail — and the invariant that lets one lexer serve all three
updated: 2026-08-02
relates: [parser]
---

# The apostrophe

ReScript gives `'` three unrelated jobs:

| form | example | lexes as |
| --- | --- | --- |
| char literal | `let c = 'a'` | `CHAR` |
| type variable | `type t<'a>` | `TICK` + ident |
| identifier tail | `let x' = 1` | one `LIDENT`/`UIDENT` |

The apostrophe is a tail character only, never a head, so a scan starting at `'`
is always either a char literal or a `TICK`.

## INVARIANT — identifier greed equals char greed

`bsc` lexes `x'a'` as ONE identifier. Widening the identifier macros therefore
cannot steal a quote that a char literal needed: wherever our identifier rule
consumes a quote, bsc's does too.

This is what lets `IDENT_TAIL` include `'` without breaking char literals, and
why the two halves cannot ship separately — a char rule alone would newly
mis-lex `let f'a' = 1` as `LIDENT CHAR`.

## Char literals

`CHAR` is a single atomic token, deliberately not split into start/content/end
the way strings are. Its content slot is exactly one character, and that is what
contains a half-typed `'`: the match fails back to `TICK` after mis-joining at
most a few characters, so an unterminated quote cannot run away.

Escapes are **width-based and alphabet-agnostic** — `\x` plus up to two
arbitrary characters, `\o` plus three, `\u` plus four, `\` plus one to three
digits, or `\` plus any single character — hex-checked only inside `\u{…}`.

The exhaustive measured table lives in executable form as the lexer fixture
`CharLiteralEscapes.res`. That file is the record; this page is not.

## Deliberate over-accepts

`''`, `'ab'` and `type t<'a'>` all lex without complaint here, and `bsc` rejects
all three.

The last is the informative case. `bsc` REJECTS `type t<'a'>`, which proves its
own scanner takes `'a'` as a char unconditionally and leaves the rejection to
its parser. We mirror that layering — so a `CHAR` token appearing in type
position is the design working, not a bug.

## A newline can live inside a CHAR

Both as raw content and inside an `\x`/`\o`/`\u` escape tail. `CHAR` is the
second token family after strings and templates that can contain a break, and
like them it does not report one: a break inside the previous *token* is not a
break to bsc. See the `sawLineBreak` table in `OVERVIEW.md`.
