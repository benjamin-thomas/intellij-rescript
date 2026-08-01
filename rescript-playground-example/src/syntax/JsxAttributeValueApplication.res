// Unbraced attribute values that are applications or containers.
//
// An unbraced value is a ReScript *primary* expression, so `Immediate(onClose)`,
// `#tag(x)`, `["one", "two"]` and `(a, b)` all belong there. `-` is the
// deliberate exception: it stays unlexable in a tag, so `neg=-1` — which the
// compiler rejects — cannot form.
//
// Open this file in the sandbox IDE (`manage/dev/runIde`) and confirm every tag
// gets JSX coloring and nothing reddens. Covered by the parser fixture
// `JsxAttributeValueApplication`.

type dismissal = Immediate(unit => unit) | Deferred

module Dialog = {
  @react.component
  let make = (~onCancel: dismissal, ~labels: array<string>, ~children) =>
    <section className="dialog" title={labels->Array.join(", ")}>
      {switch onCancel {
      | Immediate(_) => React.string("closes now")
      | Deferred => React.string("closes later")
      }}
      children
    </section>
}

// The shape reported from production: a constructor application as the value.
@react.component
let make = (~onClose: unit => unit) =>
  <Dialog onCancel=Immediate(onClose) labels=["one", "two"]>
    <p> {React.string("body")} </p>
  </Dialog>

// The punned and empty-container forms, kept as regression cover.
module StillFine = {
  @react.component
  let make = (~onCancel: dismissal) =>
    <Dialog onCancel labels=[]> <p /> </Dialog>
}
