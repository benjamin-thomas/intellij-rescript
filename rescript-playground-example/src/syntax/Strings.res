// Exercises: double-quoted strings, escape sequences, template literals,
// template interpolation, and string concat — for the lexer's distinct
// STRING_* / TEMPLATE_* states and language-injection hosts.

let greeting = "hello, world\n"
let withEscape = "tab:\there\ndouble-quote: \"x\""
let pathish = "C:\\Users\\someone\\file.txt"

let name = "Benjamin"
let count = 3

let template = `Hi ${name}, you have ${count->Int.toString} new messages.`

let multiline = `line 1
line 2
line 3`

// Plain backtick (no interpolation) — eligible for language injection.
let sqlSnippet = `SELECT id, email FROM users WHERE active = true`

let html = `<section class="card"><h1>${name}</h1></section>`

let concat = "left" ++ " — " ++ "right"
