// Exercises: regular variants with inline records, polymorphic variants,
// pattern match dispatch, or-patterns.

type shape =
  | Circle(float)
  | Rectangle({width: float, height: float})
  | Triangle(float, float, float)

type color = [
  | #red
  | #green
  | #blue
  | #rgb(int, int, int)
]

let perimeter = shape =>
  switch shape {
  | Circle(r) => 2.0 *. 3.14159 *. r
  | Rectangle({width, height}) => 2.0 *. (width +. height)
  | Triangle(a, b, c) => a +. b +. c
  }

let isPrimary = c =>
  switch c {
  | #red | #green | #blue => true
  | #rgb(_, _, _) => false
  }

let colorName = c =>
  switch c {
  | #red => "red"
  | #green => "green"
  | #blue => "blue"
  | #rgb(r, g, b) =>
    `rgb(${r->Int.toString}, ${g->Int.toString}, ${b->Int.toString})`
  }
