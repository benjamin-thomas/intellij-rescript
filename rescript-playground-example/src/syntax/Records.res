// Exercises: record types, @as field attributes, optional fields, private records,
// record literal punning, record update, structure-view "type" nodes.

type point = {
  x: float,
  y: float,
}

type user = {
  name: string,
  @as("user_id") userId: string,
  @as("created_at") createdAt: float,
  email?: string,
}

type opaque = private {
  internal: int,
  tag: string,
}

let origin: point = {x: 0.0, y: 0.0}

let translate = (p: point, dx, dy) => {x: p.x +. dx, y: p.y +. dy}

let mirror = (p: point) => {...p, x: -. p.x}

let alice: user = {
  name: "Alice",
  userId: "u-001",
  createdAt: 0.0,
  email: "alice@example.com",
}

let bob: user = {
  name: "Bob",
  userId: "u-002",
  createdAt: 0.0,
}
