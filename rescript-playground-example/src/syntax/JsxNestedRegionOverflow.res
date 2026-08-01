// Region nesting deeper than the restart int can describe.
//
// The lexer pushes a context frame per nested "region": a children region
// (`<ul>` … `</ul>`), a braced attribute value (`className={…}`), and a
// template interpolation (`${…}`). The frame stack itself is unbounded; only
// its packed form — three bytes of the restart int — is limited, and that form
// is a hint nothing reads back.
//
// Consecutive unbraced nesting is free (`<a><b><c>` shares one frame), so what
// costs frames is the alternation children > brace > children > brace. That is
// what ordinary React markup does — with only three frames the outermost
// element of components like these loses its frame.
//
// Open this file in the sandbox IDE (`manage/dev/runIde`) and confirm every tag
// gets JSX coloring and nothing reddens. Covered by the parser fixture
// `JsxNestedRegionOverflow`.

// Three regions — children > attribute > interpolation. Fits the packed form.
module ThreeRegions = {
  @react.component
  let make = (~tone: string) => <ul> {<li className={`row ${tone}`} />} </ul>
}

// Four — children > children > attribute > interpolation.
module FourRegions = {
  @react.component
  let make = (~tone: string) =>
    <ul> {<li> <span className={`cell ${tone}`} /> </li>} </ul>
}

// The same four, in the shape real code reaches it: a braced child holding a
// switch whose branch returns an element carrying an interpolated class.
@react.component
let make = (~tone: string, ~entry: option<string>) =>
  <section>
    {switch entry {
    | None => React.null
    | Some(label) =>
      <ul>
        <li className={`row ${tone}`}> {React.string(label)} </li>
      </ul>
    }}
  </section>
