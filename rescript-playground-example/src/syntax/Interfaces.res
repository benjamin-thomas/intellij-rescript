// Implementation paired with Interfaces.resi.

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
