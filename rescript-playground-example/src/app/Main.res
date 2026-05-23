// Top-level entry. Demonstrates `open` plus top-level expression statements
// (v0.4.1) so the parser surface includes "file starts with a call".

open Records

Console.log(`origin = (${origin.x->Float.toString}, ${origin.y->Float.toString})`)
Console.log(`hello, ${alice.name}`)

let _ = Variants.perimeter(Circle(2.0))
let _ = Modules.IntCounter.make(0)
