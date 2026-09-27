---
summary: Numeric spellings bsc accepts that the lexer still splits — a decimal `p` exponent and a bare hex prefix
status: todo
form: sketch
created: 2026-09-27
---

# Numeric spellings the lexer splits

## Goal

`1p3`, `1.0p3`, `0x`, `0X` and `0x_1` pass `bsc -only-parse` yet lex as two
tokens, so the editor shows the wrong PSI for a valid literal.

## Context

- Found by the hex-floats and jsx float units, and left out of both as
  separate rules.
- `bsc -only-parse` on `let x = <spelling>`, by exit status: `1p3` 0,
  `1.0p3` 0, `0x` 0, `0X` 0, `0x_1` 0. For contrast, `1...5` and `0x1p`
  exit 2.
- `ReScript.flex`: `1p3` lexes as INT `1` LIDENT `p3`; `0x` as INT `0`
  LIDENT `x`; `0x_1` as INT `0` LIDENT `x_1`. `HEX_INT` wants a hex digit
  right after the prefix, and `FLOAT`'s exponent letter is `[eE]` only.
- Decimal `1.`, `1e3`, `1.e3` and `1_000.` are covered on `jsx`
  (float-literal-forms); hex floats are on master (hex-floats).

## Acceptance Criteria

1. Each spelling above lexes as one token, of the type `bsc -dparsetree`
   gives it (`PConst_float` or `PConst_int`).
2. Lexer and parser golds carry each spelling; `RestartStates.out` updated by
   hand.
3. Existing numeric golds unchanged.

## Notes

- Which token `1p3` is (float or int) is bsc's to say: check `-dparsetree`
  before writing the gold.
