module Counter: {
  type t
  let make: int => t
  let value: t => int
} = {
  type t = int
  let make = n => n
  let value = n => n
}
