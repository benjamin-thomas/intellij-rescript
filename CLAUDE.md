## Development Tips
- ABC: Always Be Compiling -> make sure to compile often!
- Run `./gradlew verifyPluginProjectConfiguration` to catch plugin config issues

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

Run IDE with plugin: `IDEA_PROJECT=~/path/to/rescript/project ./gradlew runIde`
