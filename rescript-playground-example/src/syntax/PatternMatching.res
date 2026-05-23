// Exercises: tuple/record/list destructuring, switch with guards,
// nested patterns, exception matching, and `let` destructuring.

type http = {status: int, body: string}

let (a, b) = (1, 2)
let {status, body} = {status: 200, body: "OK"}

let classify = response =>
  switch response {
  | {status: 200} => "ok"
  | {status} if status >= 500 => "server error"
  | {status} if status >= 400 => "client error"
  | _ => "other"
  }

let first3 = xs =>
  switch xs {
  | list{} => "empty"
  | list{only} => `one: ${only}`
  | list{x, y} => `two: ${x}, ${y}`
  | list{x, y, z, ..._rest} => `three+: ${x}, ${y}, ${z}`
  }

exception NotFound(string)

let lookup = (xs, key) =>
  switch Array.find(xs, ((k, _)) => k == key) {
  | Some((_, v)) => v
  | None => throw(NotFound(key))
  }
