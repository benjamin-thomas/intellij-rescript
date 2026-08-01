// The one shape the line-break rule gets wrong, pinned deliberately.
//
// A type parameter list wrapped so that `<` opens a line is indistinguishable
// from a JSX element to a regular lexer: `array` ⏎ `<int>` and `count` ⏎
// `<div>` are the same token sequence. bsc tells them apart because it knows it
// is parsing a type; this lexer has no such context and reads both as JSX.
//
// Accepted rather than fixed, because:
//   - `rescript format` collapses `array` ⏎ `<int>` back to `array<int>`, so
//     the shape does not survive in formatted code;
//   - the damage stays inside the one declaration — the bindings after it below
//     parse normally, via structureItemRecover;
//   - the alternative is losing JSX in statement position, which is the far
//     commoner shape (every `@react.component let make` body).
//
// The whole family is affected, not just `let` annotations — return types and
// module bodies wrap the same way. The gold records two error elements. If a
// future change gives the lexer type context, this fixture is the one to flip.

let xs: array
<int> = []

type t = result
<int, string>

let untouched = "the declarations after the clash still parse"
