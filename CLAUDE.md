## Development Tips
- ABC: Always Be Compiling -> make sure to compile often!
- Run `./gradlew verifyPluginProjectConfiguration` to catch plugin config issues
- ALWAYS pass `--no-daemon` to every `./gradlew` invocation. A daemon started
  from a sandboxed agent session has a private `/tmp`; when the IDE later reuses
  that warm daemon, its init scripts (e.g. `/tmp/ijMapper1.gradle`) don't exist
  in the daemon's namespace and IDE test runs fail.

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
