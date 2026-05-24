// Exercises: module declarations, module signatures, module aliases,
// nested modules — structure-view shows the tree, breadcrumbs reflect depth.

module type Counter = {
  @@warning("-27")

  type t
  let make: int => t
  let value: t => int
  let incr: t => t
}

module IntCounter: Counter = {
  @@warning("-27")

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

type counterModule = module(Counter)
let packedCounter: counterModule = module(IntCounter)
let constrainedCounter = module(IntCounter: Counter)
let keepCounter = (counter: module(Counter)) => counter
let unpackedCounterValue = {
  let module(CounterImpl) = packedCounter
  CounterImpl.make(1)->CounterImpl.value
}

module type Store = {
  type outer
  module Inner: {
    type inner
  }
}

type typedStore<'inner, 'outer> = module(Store with
  type Inner.inner = 'inner
  and type outer = 'outer
)

let one = V.add(V.zero, (1.0, 0.0))
