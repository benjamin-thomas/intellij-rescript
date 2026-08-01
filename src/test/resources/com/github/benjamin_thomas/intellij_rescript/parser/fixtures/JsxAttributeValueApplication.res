// Unbraced attribute values across the whole class ReScript allows.
//
// An unbraced value is a *primary* expression — an atomic expression followed by
// call, index and field postfixes. The compiler says so directly: its JSX parser
// parses prop values with `parse_primary_expr ~operand:(parse_atomic_expr p)`.
//
// `(` and `[` open a region whose interior lexes as ordinary expression text,
// which is why `b=Some(-1)` works while `b=-1` still cannot form — `-` is
// unlexable in a tag, and that guards where a value STARTS (see
// JsxInvalidNegativeAttributeValue, the sentinel for not overreaching here).
//
// Two lines diverge from bsc deliberately, in opposite directions: `listValue`
// (bsc accepts, the grammar cannot spell it) and `uppercaseField` (bsc rejects,
// the grammar takes it anyway). Every other line below is accepted by both.

// Constructor application — the shape reported from production
// (`<Modal onCancel=Action(onClose)>`).
let ctorApp = <A b=Ctor(x) />
let modCtorApp = <A b=Mod.Ctor(x) />
let ctorApp2 = <A b=Ctor(x, y) />
let someApp = <A b=Some(1) />

// Plain function application.
let fnApp = <A b=f(x) />

// Polymorphic variant with a payload.
let variantApp = <A b=#tag(x) />

// Literals that need brackets or parens.
let arrayValue = <A b=[1, 2] />
// NOT supported, and the gold records the error. A braced suffix on a path is
// indistinguishable from `<A b=x {...p} />` — also legal, and far commoner —
// without whitespace sensitivity. `dict{"k": v}` is the same shape.
let listValue = <A b=list{1, 2} />
let tupleValue = <A b=(a, b) />
let parenValue = <A b=(x) />

// Postfix-free atoms, kept as regression cover.
let ctor = <A b=Ctor />
let variant = <A b=#tag />
let path = <A b=x.y />

// Postfixes chain, and an atom can be a keyword literal, a first-class module
// or an extension.
let fieldAfterCall = <A b=f(x).y />
let indexAfterCall = <A b=f(x)[0] />
let callAfterCall = <A b=f(x)(y) />
let index = <A b=arr[0] />
let boolValue = <A b=true />
let moduleValue = <A b=module(M) />
let extensionValue = <A b=%raw("x") />

// A field postfix may carry module qualification, the OCaml-inherited
// `record.Module.field` form.
let qualifiedField = <A b=person.A.name />
let qualifiedFieldAfterCall = <A b=f(x).A.B.name />

// A first-class module reaches expression context once a value region is open,
// so `module` arrives as MODULE here and as a plain path segment unbraced.
let moduleInApp = <A b=f(module(M)) />
let moduleInParen = <A b=(module(M)) />

// bsc rejects this one — a field postfix must end on a lowercase segment. The
// grammar takes it anyway: see jsxValuePostfix for why the precise rule is worse.
let uppercaseField = <A b=f(x).Bar />
