open Belt

include MyModule

external setTimeout: (unit => unit, int) => float = "setTimeout"

exception MyError(string)

let x = 1
