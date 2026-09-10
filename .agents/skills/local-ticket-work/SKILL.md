---
name: local-ticket-work
description: Pick up a ticket from _tickets/, reconcile it with the code, execute it, turn it into a spec, and drive it to done — or rework it after a flunked review.
---

# Ticket worker

Pick one ticket, execute it, track progress in its `status:` field.

**A sketch is a starting point, not a spec.** It was written before the work
was understood and the code has moved since. Expect it to be partly wrong;
your first job is to find out where. Your last job is to leave a spec: by
`done`, the ticket is what a reviewer reads.

## Layout

```
_tickets/<subject>/<NNN_name>/ticket.md
```

`status:` — `todo`, `doing`, `done`, then `passed` or `flunked` after review.
You own `todo` → `doing` → `done` and `flunked` → `doing`; the reviewer owns
`done` → `passed` / `flunked`. `form:` — `sketch` (rough, expect drift) or
`spec` (drift is a bug).

## Workflow

### 1. Select

If `_tickets/` doesn't exist, say so and stop — tickets come from
`local-ticket-create` (`.agents/skills/local-ticket-create/SKILL.md`).

```bash
grep -rH -e '^status:' -e '^form:' -e '^summary:' _tickets --include=ticket.md | sort
```

Present them grouped by subject in numeric order. **Do not auto-pick.** Then
by status:

- `todo` — continue.
- `doing` — an interrupted session, almost always. Ask before resuming.
- `flunked` — rework: go to Step 7.
- `done` — awaiting review. Nothing to do unless the human accepts without one;
  then offer the terminal step.
- `passed` — offer the terminal step, don't redo the work.

Read the whole ticket, including dated notes at the bottom from an earlier
attempt.

### 2. Reconcile

Read the files it names and the code around them. Check `created:` — the
older it is, the more has moved. Note every place the ticket no longer matches
reality: a renamed file, an assumption that no longer holds, a criterion that
has become meaningless or is already true.

A `spec` that no longer matches the code is a bug in the ticket: say so, and
fix it with the human or demote it to `sketch` before starting.

### 3. Recap and confirm

```
## Ticket: <title>  (<subject>/<NNN_name>)

### Goal
<from the ticket>

### What no longer holds
<deltas from step 2, or "nothing">

### Acceptance criteria, as I now read them
<the list, amended where step 2 requires>

### Plan
1. <file, change>

### Scope and risk
<files, unknowns>
```

Ask: **"Ready to start? Any additional instructions?"** Touch nothing — not
the status, not the code — until the human says yes. If the ticket has drifted
far, propose rewriting it instead of starting.

Once confirmed, write the amended criteria and any scope decisions back into
the ticket. Clarifying it is part of the work.

### 4. Work

Set `status: doing`. Follow `AGENTS.md`: compile as you go, `--no-daemon`
everywhere, `bsc -only-parse` by exit status for any syntax assumption. Prefer
`local-tdd` (`.agents/skills/local-tdd/SKILL.md`) when the change is testable.
Keep the ticket true as you learn: when a decision changes scope or a
criterion, edit the ticket, not just the conversation.

### 5. Verify

Walk each criterion: re-read it, check the code satisfies it, fix what's
missing. Report failures honestly.

### 6. Turn it into a spec, then signal completion

Before `status: done`, the ticket becomes a `spec`:

- Context names the files actually changed and what changed in each.
- Every criterion is final, checkable and met.
- A `## Steps` section lists what was done, in order — what and where, never
  how the session went.
- Notes hold no open questions; each is resolved or explicitly out of scope.

Then set `form: spec` and `status: done`, **only when every criterion
passes**. Otherwise leave `doing`, say which failed and why, and let the human
decide.

### 7. Rework a flunked ticket

Read the last `## Review` section. Recap each finding with your plan for it,
confirm as in Step 3, set `status: doing`. Address every finding, keep the
spec true, and add an `### Addressed` list under that review — one line per
finding. Then Steps 5 and 6 again: back to `done` for re-review.

## Terminal state

At `passed` — or at `done` when the human accepts without review — offer:

```
The work is accepted, so this ticket has served its purpose:
  _tickets/<subject>/<NNN_name>/
Git keeps it: git log --diff-filter=D -- _tickets/
Remove it? (y/N)
```

On `y`, `rm -r` the directory (no `-f`) and say it's ready to commit alongside
the work.

## When it grows

If compaction looms or you find a subsystem the ticket never mentioned:

1. **Stop.** Keep your changes.
2. Set `status: todo`.
3. Append a dated note to the ticket: what grew, how you'd split it.
4. Tell the human it needs re-splitting.

A ticket that balloons was sketched in the wrong place. Don't power through.

## Rules

- NEVER start before the human confirms
- NEVER auto-pick a ticket
- NEVER set `done` with a criterion unmet or the form still `sketch`
- NEVER set `passed` or `flunked` — those are the reviewer's
- NEVER leave `doing` behind without finishing or resetting to `todo`
- NEVER resume a `doing` ticket without asking
- NEVER run `git add`, `git commit` or `git rm` — plain `rm` is fine, it shows
  as a diff the human confirms
- Ask when the ticket is ambiguous instead of guessing
- Leave no debug code, temp files or commented-out blocks
