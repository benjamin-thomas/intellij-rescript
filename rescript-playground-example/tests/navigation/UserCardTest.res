// Mirror of src/navigation/UserCard.res. `test_<name>` aligns with the
// function-level Go-to-Test convention (ticket navigation/020).

let assertEq = (label, actual, expected) =>
  if actual == expected {
    Console.log(`OK   ${label}`)
  } else {
    Console.log(`FAIL ${label}: got ${actual}, expected ${expected}`)
  }

let test_label_withEmail = () => {
  let u = UserCard.make(~id="u1", ~displayName="Alice", ~email="a@x.io", ())
  assertEq("label with email", UserCard.label(u), "Alice <a@x.io>")
}

let test_label_withoutEmail = () => {
  let u = UserCard.make(~id="u2", ~displayName="Bob", ())
  assertEq("label without email", UserCard.label(u), "Bob")
}

let test_withEmail = () => {
  let u =
    UserCard.make(~id="u3", ~displayName="Cara", ())->UserCard.withEmail("c@x.io")
  assertEq("withEmail sets email", UserCard.label(u), "Cara <c@x.io>")
}

let run = () => {
  test_label_withEmail()
  test_label_withoutEmail()
  test_withEmail()
}
