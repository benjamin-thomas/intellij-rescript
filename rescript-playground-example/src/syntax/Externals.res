// Exercises: external declarations with @val, @module, @scope, @new,
// @send decorators — these guide the decorator association code path.

@val external windowName: string = "globalThis.name"

@val external setTimeout: (unit => unit, int) => int = "setTimeout"
@val external clearTimeout: int => unit = "clearTimeout"

@module("fs") external readFileSync: (string, string) => string = "readFileSync"

@scope("Math") @val external sqrt: float => float = "sqrt"
@scope("Math") @val external random: unit => float = "random"

@new external makeError: string => JsExn.t = "Error"

@send external trim: string => string = "trim"

let delay = (ms, callback) => {
  let _ = setTimeout(callback, ms)
}
