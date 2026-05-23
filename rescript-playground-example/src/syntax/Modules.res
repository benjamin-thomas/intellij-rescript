// Exercises: module declarations, module signatures, module aliases,
// nested modules — structure-view shows the tree, breadcrumbs reflect depth.

module type Counter = {
  type t
  let make: int => t
  let value: t => int
  let incr: t => t
}

module IntCounter: Counter = {
  type t = int
  let make = n => n
  let value = n => n
  let incr = n => n + 1
}

module Math = {
  let square = x => x * x

  module Vec2 = {
    type t = (float, float)
    let zero: t = (0.0, 0.0)
    let add = ((ax, ay): t, (bx, by): t): t => (ax +. bx, ay +. by)
  }
}

module V = Math.Vec2

let one = V.add(V.zero, (1.0, 0.0))
