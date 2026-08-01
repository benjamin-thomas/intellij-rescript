// Region nesting deeper than the restart int can describe.
//
// The lexer pushes a context frame per nested "region": a children region
// (`<ul>` … `</ul>`), a braced attribute value (`className={…}`), and a
// template interpolation (`${…}`). The frame stack is a plain array with no
// bound; only its packed form — bits 8..31 of the restart int, three bytes —
// is limited, and that form is a hint nothing reads back.
//
// Consecutive unbraced nesting is free: `<a><b><c>` shares one frame via a
// child count. A frame is pushed only when a region opens inside something
// that is not already a depth-0 children region — so it is the alternation
// children > brace > children > brace that costs frames, which is exactly what
// ordinary React markup does, and why three was never enough.
//
// bsc accepts every line below.

// Three regions — children > attribute > interpolation. Fits the packed form.
let threeRegions = <ul> {<li className={`row ${tone}`} />} </ul>

// Four — children > children > attribute > interpolation. Past what the packed
// form holds: a packed-only stack drops the `<ul>` frame here and unravels the
// element at `</ul>`, far from the cause.
let fourRegions = <ul> {<li> <span className={`cell ${tone}`} /> </li>} </ul>

// The other packed capacity: consecutively nested unbraced elements share one
// frame whose packed count holds seven, and real ReScript reaches nine — a
// count capped at the packed width pops a live frame on the ninth `</…>`.
let deepNesting = <a><b><c><d><e><f><g><h><i> {"x"->React.string} </i></h></g></f></e></d></c></b></a>

// The same four, in the shape real code reaches it: a braced child holding a
// switch whose branch returns an element carrying an interpolated class.
let realistic =
  <section>
    {switch entry {
    | None => React.null
    | Some(row) =>
      <ul>
        <li className={`row ${row.tone}`} />
      </ul>
    }}
  </section>
