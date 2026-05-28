// Exercises: mutually recursive declarations chained with `and` —
// `type … and …`, `let rec … and …`, and a decorated `and` continuation.
// A decorated continuation used to be a hard parse error, and a bare
// continuation parsed but was absorbed into the previous declaration instead
// of getting its own PSI node. Structure view (Alt+7) should now list every
// member — `expr`, `stmt`, `legacyProgram`, `evalExpr`, `evalStmt`,
// `isEven`, `isOdd` — as siblings.

// Mutually recursive types: expressions contain statements and vice versa.
type rec expr =
  | Lit(int)
  | Add(expr, expr)
  | Run(stmt)
and stmt =
  | Return(expr)
  | Seq(stmt, stmt)
// A decorator on the continuation binds to `legacyProgram`, not to `stmt`.
@deprecated("kept for the parser smoke-test; prefer `stmt`")
and legacyProgram = stmt

// Mutually recursive functions over the types above.
let rec evalExpr = e =>
  switch e {
  | Lit(n) => n
  | Add(a, b) => evalExpr(a) + evalExpr(b)
  | Run(s) => evalStmt(s)
  }
and evalStmt = s =>
  switch s {
  | Return(e) => evalExpr(e)
  | Seq(a, b) => evalStmt(a) + evalStmt(b)
  }

// The classic even/odd pair — mutually recursive `let rec … and …`.
let rec isEven = n => n <= 0 ? n == 0 : isOdd(n - 1)
and isOdd = n => n <= 0 ? false : isEven(n - 1)

let answer = evalStmt(Seq(Return(Lit(20)), Return(Add(Lit(11), Lit(11)))))
let tenIsEven = isEven(10)
