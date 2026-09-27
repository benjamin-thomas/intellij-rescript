let teams = [{name: "Acme", logo: module(Icons.Logo)}]
let r = {...base, logo: module(M)}
let demos = {"AccordionBasic": module({ let x = 1 })}
let f = x => switch x { | Some({logo: module(Logo), name}) => name | None => "" }
type t = {logo: module(T)}
