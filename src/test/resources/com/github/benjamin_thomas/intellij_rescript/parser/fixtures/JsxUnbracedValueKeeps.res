// Unbraced-value shapes that already parse, and must keep parsing this way.

// With a space, the brace after `list` opens a spread attribute, not a list.
let spreadAfterList = <A b=list {...p} />
let bracedList = <A b={list{1}} />
let postfixes = <A b=x.y c=f(1) d=[1] e=#tag f=%raw("x") g=Some(1).x />

// A line comment right after a value, then an empty one: `//` stays a comment.
let lineComment = <A b=x // c
/>
let emptyLineComment = <A b=x //
/>
