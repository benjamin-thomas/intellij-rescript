// Exercises: numeric literal forms (hex, octal, binary, underscores,
// scientific notation, BigInt) and the v12 operator set.

let dec = 1_000_000
let hex = 0xFF_FE
let octal = 0o755
let binary = 0b1010_1010
let big = 9_999_999_999_999_999n

let pi = 3.141_592_653
let avogadro = 6.022e23
let small = 1.5e-9

let bitwiseAnd = 0xF0F0 &&& 0x0FF0
let bitwiseOr = 0xF000 ||| 0x0F00
let bitwiseXor = 0xFFFF ^^^ 0xF0F0

let power = 2.0 ** 10.0
let strictEq = 1 === 1
let strictNeq = 1 !== 2
