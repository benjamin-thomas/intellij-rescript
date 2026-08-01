// JSX in statement position — an element that follows a completed expression.
//
// A `<` glued to a name is a tag once a line break separates it from the
// previous significant token, and the comparison operator otherwise. bsc
// agrees: `a<b` and `a <b` are comparisons, while `a` ⏎ `<b` is an element
// (which bsc then rejects as unterminated).
//
// Without the line-break signal every element below lexes as comparison,
// because the token before it — `1`, `}`, `)` — is an "expression end".
// The lexer fixture of the same name covers the surrounding edge cases
// (comments, CR-only, a newline inside the previous token).

let count = 1

<div />

// The `@react.component let make` shape: the element trails a `let` ending in `}`.
let render = () => {
  let label = switch count {
  | 0 => "none"
  | _ => "some"
  }

  <div />
}

// Siblings inside a statement-position element. Lexed as comparison these
// alternate between real tags and soup, and the trailing `</div>` lands after a
// bare `>` — the one shape that surfaces an error, on the closing tag rather
// than on the `<div>` responsible for it.
let make = () => {
  Console.log("mounted")

  <div>
    <A />
    <B />
  </div>
}

// Comparison stays comparison — same line, spaced and glued.
let isSmall = (n: int) => n < 10
let lt = (a, b) => a<b
