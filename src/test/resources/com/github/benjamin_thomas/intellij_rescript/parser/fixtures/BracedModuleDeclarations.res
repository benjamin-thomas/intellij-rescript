module A = {
  module B = {}
  module type T = {}
  @foo module C = {}
  let module(M) = x
  let y = module(M)
}
let v = {
  let z = module(M)
  module N = {}
  z
}
