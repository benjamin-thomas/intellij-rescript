// JSX — a JSX element used as another element's attribute value.
//
// The shape `<Outer prop={<Inner attr="literal" />} />` used to defeat the
// plugin's permissive parser: the `/` of the inner `/>` was lexed as the start
// of a regex literal (a closing string / template / `}` wasn't treated as an
// "expression end"), so the bogus regex swallowed the surrounding `/>` and `}`
// and left the braces unbalanced — marking the whole file red in the project
// tree, with no inline squiggle pointing at the cause.
//
// Open this file in the sandbox IDE (`manage/dev/runIde`) and confirm the file
// and the project-tree node stay un-reddened. Covered by the parser fixtures
// `JsxElementAsAttributeValue*` and `JsxNestedElementAsAttributeValue`.
//
// NOTE: do not name this file `Jsx.res` — that shadows ReScript's built-in
// `Jsx` runtime module that the JSX transform references, and the file fails to
// type-check.

module Icon = {
  @react.component
  let make = (~name: string) =>
    <span className="icon" title=name> {React.string("[" ++ name ++ "]")} </span>
}

module Button = {
  @react.component
  let make = (~label: string, ~icon: React.element) =>
    <button className="btn"> icon {React.string(label)} </button>
}

module Card = {
  @react.component
  let make = (~header: React.element) => <section className="card"> header </section>
}

// JSX element as an attribute value; the inner element carries a string attribute.
let saveButton = <Button label="Save" icon={<Icon name="disk" />} />

// Same shape, but the inner attribute is a template literal.
let undoButton = <Button label="Undo" icon={<Icon name=`arrow` />} />

// Nested two levels deep: a JSX-as-attribute value that itself contains a
// JSX-as-attribute value. The middle `/>` follows a `}`, which is the case
// that needed RBRACE to count as an expression end.
let editCard = <Card header={<Button label="Edit" icon={<Icon name="pencil" />} />} />
