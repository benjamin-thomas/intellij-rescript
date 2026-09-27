let listValue = <A b=list{1, 2} c=3 />
let nestedDelimiters = <A b=list{[1], 2} />
let emptyList = <A b=list{} />
let gluedSelfClose = <A b=list{1, 2}/>
let withChildren = <A b=list{1, 2}> child </A>
let dictValue = <A b=dict{"k": 1} />
// Glued, the spread belongs to the list: bsc reads the value as `xs`.
let listSpread = <A b=list{...xs} />
