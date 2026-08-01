// JSX in statement position — the element a block returns, sitting after a
// preceding statement rather than directly after `=` or `=>`.
//
// A `<` glued to a name is a tag once a line break separates it from the
// previous significant token, and the comparison operator otherwise. Without
// that signal every element here lexes as comparison, because the token before
// it — `}`, `)`, a literal — is an "expression end"; `make` then reports an
// error on its `</section>`, four lines below the `<section>` responsible.
//
// Open this file in the sandbox IDE (`manage/dev/runIde`) and confirm every tag
// gets JSX coloring and nothing reddens. Covered by the parser fixture
// `JsxStatementPosition`.

module Row = {
  @react.component
  let make = (~label: string) => <li> {React.string(label)} </li>
}

// The `@react.component let make` shape: a `let` ending in `}`, then the
// returned element.
@react.component
let make = (~name: string) => {
  let title = switch name {
  | "" => "untitled"
  | other => other
  }

  <section>
    <Row label=title />
    <Row label="second" />
  </section>
}

// Same defect after a call — any expression end will do.
let afterCall = () => {
  Console.log("side effect")

  <Row label="after a call" />
}

// Top level, where the element is absorbed by the preceding binding.
let count = 1

<Row label="top level" />->ignore

// Comparison stays comparison — same line, spaced and glued.
let isSmall = (n: int) => n < 10
let lt = (a: int, b: int) => a<b
