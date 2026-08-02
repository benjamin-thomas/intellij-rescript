type t<'a> = option<'a>
type t2<'a, 'b> = ('a, 'b)
type t3<'A> = list<'A>
type t4<'a> = 'a constraint 'a = int
let f: 'a => 'a = x => x
let g: '_a => '_a = x => x
