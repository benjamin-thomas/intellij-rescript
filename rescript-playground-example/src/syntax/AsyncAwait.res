// Exercises: async/await keywords, try/catch around await, promise chaining
// through `->` pipe. Newer keywords (async, await, try, catch) exercise the
// v0.2.0 lexer additions.

let later = (value, ms) =>
  Promise.make((resolve, _reject) => {
    let _ = Externals.setTimeout(() => resolve(value), ms)
  })

let fetchUserName = async id => {
  let _ = await later((), 0)
  `user-${id->Int.toString}`
}

exception LookupFailed(string)

let safeLookup = async key => {
  try {
    let name = await fetchUserName(key)
    if key < 0 {
      throw(LookupFailed(`negative key: ${key->Int.toString}`))
    }
    Ok(name)
  } catch {
  | LookupFailed(msg) => Error(msg)
  }
}

let pipelineExample = () =>
  fetchUserName(1)
  ->Promise.then(async name => `hello, ${name}`)
  ->Promise.thenResolve(Console.log)
