type packed<'inner, 'outer> = module(MT with
  type Inner.inner = 'inner
  and type outer = 'outer
)
