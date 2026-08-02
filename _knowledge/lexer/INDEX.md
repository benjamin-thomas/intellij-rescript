# Lexer

- **OVERVIEW.md** — JFlex grammar, adapter pattern, token types, states, pushback, syntax highlighting, testing strategy
- **APOSTROPHE.md** — The three roles of `'` (char literal, type variable, identifier tail) and why one lexer can serve all three
- **RESTART_STATE.md** — Why template interpolation needs packed lexer restart state, how IntelliJ restart works, and how the bit packing is encoded
- **STRING_HANDLING.md** — Elm-style lexer states for strings/templates, two token families, design rationale
- **scripts/packed_stack_demo.rb** — Interactive Ruby demo for the packed interpolation stack
