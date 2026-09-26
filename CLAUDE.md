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

## Skills and tickets

Project skills live in `.agents/skills/local-*/SKILL.md`; `.claude/skills/`
holds symlinks to them. Where a `local-` skill overlaps a global one, prefer
the `local-` one.

- `local-ticket-create` / `local-ticket-work` / `local-ticket-review` — planned
  work under `_tickets/<subject>/<NNN_name>/ticket.md`. Tickets start as
  sketches (rough steps) and end as specs; the reviewer holds the diff against
  a spec and sets `passed` or `flunked`.
- `local-tdd` — the RED / GREEN / REFACTOR loop for this codebase; each green
  proposes a commit message, committed only when the user says so.
- `local-tdd-tcr` — the same loop run semi-autonomously for a budget of rounds,
  under test && commit || revert, committing on the current feature branch.
- `local-orchestrate` / `local-worker` / `local-defect-review` — one goal run
  in parallel, without tickets: `manage/agents` gives each unit of work its own
  worktree (`../<repo>.worktrees/`) and sandboxed herdr session, where a worker
  runs TCR from a brief and a reviewer agent fails only reproducible defects;
  the orchestrator merges ready branches into the subject branch.
- `local-prep-release` — cut a release.

## Git

Git and publishing are the user's. Never push, never publish, and don't stage
or commit: propose a commit message, and the user stages and commits. Commit
yourself only when the user grants it for the session, answers `c` at a
`local-tdd` checkpoint, under `local-tdd-tcr`, or on the branch
`local-orchestrate` or `local-worker` gives you.

A commit message is an imperative title of **50 characters or fewer** — the
log is skimmed — with a body only when the title cannot say it all.

Ticket work goes on a feature branch (see `local-ticket-work`), which the user
lands on master with `git merge --no-ff`, never squashed.

Writing to `.git` needs a sandbox launched with `sandbox-agent --git-rw`. Check
with `test -w "$(git rev-parse --absolute-git-dir)"`; when it is read-only,
give the user the commands instead.

## Testing

Run tests: `./gradlew test`

Commit (or separately stage) characterization/baseline fixtures BEFORE
implementing the behavior change they guard — a reviewer must be able to
verify from history that the baseline predates the change.

Run IDE with plugin: `IDEA_PROJECT=~/path/to/rescript/project ./gradlew runIde`
