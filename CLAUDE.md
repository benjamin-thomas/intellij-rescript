## Development Tips
- ABC: Always Be Compiling -> make sure to compile often!
- Run `./gradlew verifyPluginProjectConfiguration` to catch plugin config issues
- ALWAYS pass `--no-daemon` to every `./gradlew` invocation. A daemon started
  from a sandboxed agent session has a private `/tmp`; when the IDE later reuses
  that warm daemon, its init scripts (e.g. `/tmp/ijMapper1.gradle`) don't exist
  in the daemon's namespace and IDE test runs fail.

## Comments

A comment documents only what is NOT evident from reading the code. It must never
document the current coding session.

Delete on sight, including in code you are only passing through:
- Process narration — "in this cycle", "verified against bsc", "the gold is
  hand-written", "pin returns later", "now that X landed", "TODO from the review".
- Restatements of what the line plainly says.
- History. Git already has it.

Keep only what a reader could not derive: a non-obvious language constraint, a
trap, or the reason something is deliberately NOT done the obvious way.

## Founding principle: prefer over-accepting

An IDE plugin is not a compiler. Its two failure directions cost wildly
different amounts:

- **over-accept** (plugin fine, `bsc` errors) — the user sees nothing; the
  compiler reports it anyway.
- **under-accept** (plugin errors, `bsc` fine) — a red squiggle on valid code,
  with no second opinion in the editor.

So when the two conflict, over-accept. Narrowing a rule to match the compiler
exactly is NOT automatically an improvement — check the gold file, because a
tighter rule that ends a value early produces looser PSI and usually reports
nothing extra.

Each layer's job:
- **lexer** — segmentation only. Rejects nothing, never throws, contains damage
  on half-typed code.
- **parser (BNF)** — build a usable tree from broken input. Only context-free
  violations, and only as a side effect.
- **annotator / inspection** — where rejection belongs. Anything needing name
  comparison or case rules (`<div></span>`) goes here, not in the grammar.

Full reasoning and the measurements behind it:
`_knowledge/architecture/RESPONSIBILITIES.md`.

## ReScript syntax questions

Never infer what the language accepts — ask the compiler:

```
rescript-playground-example/node_modules/.bin/bsc -only-parse file.res
```

Check the EXIT STATUS, not the visible output. `bsc` prints a leading blank
line before a syntax error, so harnesses that sample the first line (or use a
`${out:-LEGAL}` fallback) report errors as legal. That mistake put a
compiler-invalid form into a specification once already.

Do this before encoding any syntax assumption in a grammar rule, a gold file,
or a comment.

## Knowledge base

The `_knowledge/` directory contains domain knowledge about how JetBrains language
plugins work and how this plugin is built. Reading it is optional — consult when
you need context on a specific topic.

- `_tickets/todo/` — planned work, decomposed into context-sized tickets (see `_tickets/todo/<subject>/<NNN_name>/ticket.md`)
- `_knowledge/INDEX.md` — entry point to the knowledge base

Knowledge can become stale. If you discover that something in `_knowledge/` is
wrong or outdated, update or remove it. Use `/condense-knowledge` at the end of
a session to capture new learnings and correct existing ones.

## Testing

Run tests: `./gradlew test`

Commit (or separately stage) characterization/baseline fixtures BEFORE
implementing the behavior change they guard — a reviewer must be able to
verify from history that the baseline predates the change.

Run IDE with plugin: `IDEA_PROJECT=~/path/to/rescript/project ./gradlew runIde`
