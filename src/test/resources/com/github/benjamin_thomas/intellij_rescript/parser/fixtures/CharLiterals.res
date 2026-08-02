let c = 'a'
let n = f('a')
let l = list{'a', 'b'}
let describe = c =>
  switch c {
  | 'a' => 1
  | 'b' | 'c' => 2
  | 'd' .. 'z' => 3
  | _ => 0
  }
let e = '\n'
let q = '''
let j = <Foo c='a' />
let k = <div> 'a' </div>
let m = <div> {'a'} </div>
let x' = 1
