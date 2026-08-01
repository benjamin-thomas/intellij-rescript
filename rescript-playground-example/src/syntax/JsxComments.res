// Comments in every JSX position. The compiler accepts all of them: both
// comment types are legal in an opening tag, in a closing tag, and between
// children.
//
// Open this file in the sandbox IDE (`manage/dev/runIde`) and confirm no
// comment reddens and no prose after one is coloured as an attribute. Covered
// by the parser fixture `JsxComments`.

module Row = {
  @react.component
  let make = (~label: string, ~weight: int) =>
    <li className="row"> {React.string(`${label}:${weight->Int.toString}`)} </li>
}

// A line comment documenting the next attribute — the reported shape.
@react.component
let make = () =>
  <ul>
    <Row
      // the text shown on the left
      label="alpha"
      weight=1
    />
    <Row /* inline, before the attribute */ label="beta" weight=2 />
    <>
      // about the child below
      <Row label="gamma" weight=3 />
    </>
  </ul /* the list ends here */>
