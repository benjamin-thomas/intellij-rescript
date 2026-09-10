---
name: local-ticket-create
description: Turn planned work into context-sized tickets under _tickets/. A ticket starts as a sketch, not a spec; it becomes a spec only as it is worked to done.
---

# Ticket creator

Capture what we want to do next, in pieces small enough to pick up cold,
without pretending to know more than we do.

## What a ticket is

A ticket's `form:` says how to read it.

**`sketch` — a rough step, not a specification.** The default. It records the
intent, the rough shape and where to start looking — as understood on the day
it was written. Whoever picks it up re-derives the details against the code as
it is then, and should expect the sketch to be partly wrong. So write less
than you know: anything precise enough to be wrong later — line numbers, a
step-by-step plan, exact rule names — is a liability. Name files and concepts;
let the worker read the code. The only thing that must be right is the
**boundary**: what is in, what is out, and what "done" roughly looks like.

**`spec` — the real thing.** Every criterion is final and checkable, Context
names the exact files and what changes in each, and a `## Steps` section lists
the concrete changes in order. A reviewer holds the diff against it.

Tickets are born as sketches and turned into specs by `local-ticket-work` on
the way to `done`: having no spec is normal *before* the work and
unjustifiable *after* it. Write a `spec` from the start only when boundary,
files, criteria and steps are all known today (a mechanical rename, a version
bump), and say so.

## Layout

```
_tickets/<subject>/<NNN_name>/ticket.md
```

`status:` runs `todo` → `doing` → `done`, then `passed` or `flunked` after
review. You write at `todo`; `local-ticket-work`
(`.agents/skills/local-ticket-work/SKILL.md`) and `local-ticket-review` own
the rest. `form:` is `sketch` or `spec`; `local-ticket-work` advances it.
Accepted tickets are deleted — git history is the archive.

## Workflow

### 1. Survey

```bash
grep -rH -e '^status:' -e '^form:' -e '^summary:' _tickets --include=ticket.md | sort
```

Subjects and order fall out of the sorted paths. Reuse a subject when the work
touches the same surface.

### 2. Ask only what the code can't answer

One or two rounds at most: which surface, what should change, what is
explicitly out, what done looks like. Skip anything the request already said.

### 3. Look at the code

Open the files you will name. Don't cite what you haven't read.

### 4. Size

One ticket fits one context window without compaction — that also keeps the
diff reviewable. Split independent concerns (lexer, parser, inspection); keep
one conceptual change together even if it touches ten files. More than 3–7
acceptance criteria is a smell.

### 5. Bugs need a reproduction

If the ticket says something is broken, it carries a `.res` snippet, what the
plugin does, and what `bsc -only-parse` says — by exit status, see
`AGENTS.md`. No reproduction, no bug ticket: file it as an investigation.

### 6. Write

Number from `010` in tens per subject, `NNN_lowercase-hyphenated`, never
nested, never renumbered.

```markdown
---
summary: One line
status: todo
form: sketch
created: YYYY-MM-DD
---

# <Title>

## Goal

<One or two sentences: what and why.>

## Context

<Files and concepts to start from. Assume the reader has read nothing else.
No line numbers, no plan.>

## Reproduce

<Bugs only: the snippet, what happens, what bsc says. Drop otherwise.>

## Acceptance Criteria

1. <checkable, but expect it to be revised on pickup>

## Steps

<Spec only: the concrete changes, in order — what and where, never how the
session went. Absent in a sketch.>

## Notes

<Constraints, edge cases, decisions from the conversation. Out-of-scope items
go here so they don't leak back in.>
```

`created` tells the worker how stale to expect it. Criteria are the best
current guess at "done": checkable ("`<` inside a type annotation no longer
auto-closes"), not vague ("brace matching works"), but revisable.

### 7. Confirm, then write

Show the proposed tree with each ticket's goal and criteria. Incorporate
feedback. **Only then write files.**

## Rules

- NEVER implement — you write tickets, someone else executes
- NEVER file a bug ticket without a reproduction
- NEVER put steps in a sketch, or line numbers in any ticket
- NEVER nest tickets or record the subject in frontmatter — the path is
  authoritative
- NEVER renumber existing tickets
- NEVER run `git` write commands — the human stages and commits
- Present the full plan before writing any file
