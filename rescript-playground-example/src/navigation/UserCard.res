// Paired with tests/navigation/UserCardTest.res — exercises the planned
// Go-to-Test handler (src/Foo.res ↔ tests/FooTest.res).

type t = {
  id: string,
  displayName: string,
  email: option<string>,
}

let make = (~id, ~displayName, ~email=?, ()): t => {
  id,
  displayName,
  email,
}

let label = (user: t) =>
  switch user.email {
  | Some(e) => `${user.displayName} <${e}>`
  | None => user.displayName
  }

let withEmail = (user: t, email): t => {...user, email: Some(email)}
