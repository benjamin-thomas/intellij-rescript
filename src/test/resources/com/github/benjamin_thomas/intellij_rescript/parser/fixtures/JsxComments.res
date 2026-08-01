// Comments in every JSX position. The compiler accepts all of them, and the
// parser needs no rule for any of them: both comment types are in
// `getCommentTokens()`, so PsiBuilder skips them everywhere for free.
//
// A block comment records the state it was entered from and returns there. It
// can do that with a plain field rather than one cloned state per JSX position,
// because no rule inside the comment state emits a token — the whole comment is
// consumed inside one `advance()`, so no restart can land inside one. Strings
// and templates DO emit tokens mid-state, which is why they needed
// IN_TAG_STRING / IN_CHILD_STRING clones and this does not.

// The reported shape: a line comment documenting the next attribute.
let lineInTag =
  <A
    // what this attribute is for
    b=1
  />

let blockInTag = <A /* what this is for */ b=1 />

let lineInChildren =
  <A>
    // about the child below
    {x}
  </A>

let blockInChildren = <A> /* about the child */ {x} </A>

let blockInClosingTag = <A> {x} </A /* about the element */>
