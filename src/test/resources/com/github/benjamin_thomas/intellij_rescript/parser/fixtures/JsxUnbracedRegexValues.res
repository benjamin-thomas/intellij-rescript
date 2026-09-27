let plain = <A b=/re/ />
let flagsThenAttribute = <A b=/a\/b/gi c=1 />
let optional = <A b=?/re/ />
let opensChildren = <A b=/re/> x </A>
// The regex, then `/>`: not a line comment.
let gluedSelfClose = <A b=/re//>
// After a value, `/>` still ends the tag although a `/` follows on the line.
let laterOnTheLine = <div> <A b=x /> <B c=/re/ /> </div>
