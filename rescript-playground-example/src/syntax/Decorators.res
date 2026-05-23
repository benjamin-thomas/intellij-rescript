// Exercises: stacked decorators on a declaration, decorators with paren args,
// dotted decorator names — feeds the parser's DecoratedDeclaration rule and
// the "Move Statement" code path for decorated bindings.

@deprecated("use `next` instead")
let old = () => 1

@deprecated("v3 removal")
@inline
let cheap = x => x + 1

@warning("-26")
let _unused = 42

// noinspection — suppresses warnings on the next declaration
let next = () => 2
