---
name: local-ticket-review
description: Hold finished work against its done ticket, which must be a spec. Read-only on code; writes only the verdict and the status.
---

# Ticket reviewer

Hold the diff against the spec that describes it.

Run this in a **fresh session**. If this conversation did the work, say so and
stop — a reviewer who wrote the code checks its own assumptions.

You never touch code. Your only writes are the ticket's `status:` and a dated
`## Review` section holding your verdict.

## Preconditions

```bash
grep -rH -e '^status:' -e '^form:' -e '^summary:' _tickets --include=ticket.md | sort
```

Review only `status: done` **and** `form: spec`. Anything else isn't ready:
name the field that is off and stop. A `done` ticket still a `sketch` was
closed without being turned into a spec — that is `local-ticket-work`'s job.

If the ticket already has `## Review` sections, this is a re-review: read
each one and its `### Addressed` list. Re-check every earlier finding, then
review the whole diff as if new.

## Workflow

### 1. Get the diff

Ask which it is unless obvious:

```bash
git diff HEAD && git status --short     # uncommitted, in the tree
git diff master...HEAD                  # committed on a branch
```

Read the whole diff, not a stat.

### 2. Hold the diff against the spec

Three passes:

- **Criteria → evidence.** For every acceptance criterion, find the hunk or
  test that satisfies it. Met, not met, or can't tell — never "probably".
- **Steps → hunks.** Every step in `## Steps` maps to changes in the diff.
- **Hunks → steps.** Every hunk is explained by a step. What isn't is scope
  creep or a step the spec forgot — say which.

Then check the work against `AGENTS.md`: comments narrating the session,
grammar tightened where over-accepting was right, a `.res` fixture never
checked against `bsc`, `./gradlew` without `--no-daemon`.

### 3. Run what the spec claims

```bash
./gradlew --no-daemon test
```

A criterion that names a manual check: say you did not perform it.

### 4. Record the verdict

Append to the ticket:

```
## Review <YYYY-MM-DD>

| # | criterion | verdict | evidence |
| 1 | …         | met     | <file / test> |

### Steps vs diff
<unmatched steps, unexplained hunks, or "all accounted for">

### AGENTS.md
<findings, or "nothing">

### Tests
<pass, or the failure verbatim>

### Verdict
passed | flunked — <one line why>
```

`passed` only when every criterion is met, every hunk is accounted for and
the tests pass. Set `status:` to the verdict, show the section to the human,
and stop. `local-ticket-work` reworks a `flunked` ticket and removes a
`passed` one once the human accepts.

## Rules

- NEVER review work this session did — stop and ask for a fresh session
- NEVER edit code — findings only; fixes go through `local-ticket-work`
- NEVER write anything but the `## Review` section and `status:`
- NEVER review a ticket that is not both `done` and a `spec`
- NEVER mark a criterion met without naming the evidence
- NEVER run `git` write commands — the human stages and commits
