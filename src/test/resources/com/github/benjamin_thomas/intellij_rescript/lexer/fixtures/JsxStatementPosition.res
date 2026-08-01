let count = 1
<div />
let glued = a<b
let spaced = a < b
let wrapped = c
<D />
let afterLineComment = 1
// note
<E />
let afterBlockComment = 1
/* note */ <F />
let breakInsideComment = 1 /* note
*/ <G />
let breakInsideNestedComment = 1 /* a /* b */ c
*/ <H />
let sameLineComment = a /* note */ <b
let breakInsidePrevToken = "a
b" <c
