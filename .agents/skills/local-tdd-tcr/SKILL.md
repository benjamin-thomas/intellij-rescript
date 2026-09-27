---
name: local-tdd-tcr
description: Semi-autonomous TDD for the intellij-rescript plugin — run an approved test list for a budget of N rounds without checkpoints, under TCR (test && commit || revert), making one passing commit per round on the current feature branch so the human reviews each round's diff afterwards. Use when the user grants a round budget ("run 10 rounds", "TCR this"); use local-tdd when they want to steer every step.
---

# TDD under TCR — a budget of rounds, reviewed afterwards

`local-tdd` stops at every red and every green so the human can steer. This
skill trades those checkpoints for a **budget**: the human approves a test list
once, grants N rounds, and comes back to N small commits to review one diff at
a time — keeping the good prefix, throwing the rest away.

**Each commit is one behaviour, test and code together, and the plugin works at
every one of them.** No commit fails its tests, so any prefix of the batch can
be kept as it is.

What keeps that safe is **TCR — test && commit || revert.** Every attempt ends
in exactly one of two ways: the gate passes and the work is kept, or it fails
and the work is thrown away. Nothing is patched up after a failure, so the
cheapest way through is always the smallest step. That discipline applies to
both sides: a test that doesn't fail the way it should is discarded, and so is
code that doesn't make it pass.

Write tests as `.agents/skills/local-tdd/test-style.md` says — read it before
the first round. That file is all you need from `local-tdd`: its checkpoints
and commit rules are for a human-driven loop, and this skill replaces them.

## Git: the one exception

Everywhere else, you commit only when the user says so. Invoking this skill is
the user granting you commits **on the current feature branch, for this batch
only** — on top of the base commit the batch started from. Nothing else
changes: never commit on master, never push, never touch another branch, never
rewrite a commit from before the base, never merge.

## 0. Before the first round

1. **Git is writable — check this first.** Sessions start with `.git`
   read-only unless the sandbox was launched with `sandbox-agent --git-rw`:

   ```bash
   test -w "$(git rev-parse --absolute-git-dir)" && echo writable || echo read-only
   ```

   `read-only`: stop before anything else, and ask the user to relaunch with
   `--git-rw`. Without it nothing can be committed, so no round can end.
2. **Clean tree.** `git status --porcelain` must be empty. Reverts run
   `git reset --hard`, `git restore` and `git clean -fd`, which would destroy
   the user's uncommitted work — so if anything is there, stop and ask. Never
   stash it yourself.
3. **Green baseline.** Run the gate (below) on the untouched tree. TCR cannot
   start from red: if the gate fails, stop and report it.
4. **Feature branch — required.** Never master: a batch is a run of micro
   commits, which a `--no-ff` merge keeps off master's first-parent history but
   which would flood it if made there directly. If you are on master, propose a
   branch name with the test list; on yes, `git switch -c <branch>`.
5. **Test list.** Read the ticket or the request, then propose the behaviours
   to drive out, one per round, simplest first — each a single sentence a test
   can check. Settle every syntax assumption with `bsc` now, by exit status, as
   `CLAUDE.md` says: the list is where a wrong one would get encoded. Mark as
   **baseline** any item that pins current behaviour a later round must keep
   (see "Baseline rounds"). Present the list with the budget and the branch,
   and ask once: **"Start? (y/N)"** Working a ticket, this is
   `local-ticket-work`'s "Ready to start?" — one question, not two. A
   `local-worker` dispatched in autonomous mode does not ask: the dispatch
   is the approval.
6. **The ticket, when working one.** It lives in this repo, so a revert would
   take its edits with it. After the yes, write its reconciled state — the
   amended criteria, `status: doing` — and commit it alone:
   `git add _tickets/<subject>/<NNN_name> && git commit -m "Update ticket <NNN_name>"`.
   Then leave it untouched until the report.
7. **Base.** Note the base commit (`git rev-parse HEAD`), after the ticket's:
   the review range starts there, and nothing before it is yours to touch.

After that yes, there are no more questions until the batch ends.

## The gate

```bash
rm -rf src/main/gen && ./gradlew --no-daemon test
```

Its **exit status** decides. It runs the whole suite — under a minute — because
a grammar change often moves other gold files, and those must fail the round.

`src/main/gen` is gitignored and Grammar-Kit never deletes what it generated,
so no revert touches it: a class left over from a renamed or removed rule can
break the build, or keep code that still references it compiling. The next
compile regenerates it.

Gradle prints only the failing test's name and exception class. The assertion
message is in `build/test-results/test/TEST-<class>.xml`; a gold mismatch has
no message worth reading, but its actual text is written to
`build/gold-actual/<the gold's path under src/test/resources>`: diff that
against the gold. A later run of a single class (`--tests`) wipes the other
classes' XML, so read a failure before running anything else.

## A round

A round is one behaviour from the test list. It ends in exactly one commit, or
in none if it is abandoned; the budget counts those commits.

Nothing is committed until the round passes, so the **index** holds its
checkpoint: once the test fails the right way it is staged, and a failed green
attempt falls back to exactly that.

### RED — the test must fail, and fail for the right reason

1. Write the tests for the next behaviour: one, or several when one cause
   shows in several examples, each of which must fail. Add whatever stubs they
   need to compile (a red step is never a compile error). For a gold-file test, write
   the `.res` fixture and hand-write the gold you expect. Never let the
   framework write it: `assertSameLinesWithFile` turns a missing gold into a
   copy of the current output, and the test then passes on the very code it
   was meant to drive.
2. Run the gate. It must fail **on the tests this round wrote or changed —
   every one of them, and only those** — with the assertions you expect.
3. If so, stage it — `git add -A` — and keep the failure's key line for the
   report: it is the evidence that the test demands the code.
4. If it fails anywhere else, or for another reason, discard it:
   `git reset --hard HEAD && git clean -fd`. A test failing for the wrong
   reason proves nothing. If it passes, the behaviour already holds: it is
   not a RED but a candidate baseline (below), or nothing.

### GREEN — the minimal code, and only code

1. Write the minimal production code to pass. **Do not touch anything under
   `src/test/`** — the staged test fixed what "done" means for this round.
2. Run the gate. Green → commit the round, test and code together:

   ```bash
   git add -A && git commit -m "<the behaviour, in the imperative, 50 characters or fewer>"
   ```

   Red → fall back to the staged test and try a smaller step:

   ```bash
   git restore . && git clean -fd
   ```

3. If the test itself turns out to be wrong, the round is wrong: discard it
   (`git reset --hard HEAD && git clean -fd`) and write a better red. That
   counts as a failed attempt.

A green that passes the round's tests but moves another gold file is red like
any other. If the new tree is the right one — what you would have written by
hand, and no looser than the old (no node lost, no error element gained) — the
round's red was incomplete: redo it with that gold updated too, and name the
file in the report. Otherwise the code regressed: fall back and try smaller.

The message says what the commit does — "Parse hyphenated JSX tag names" — like
any other commit on the branch. No round numbers, no red/green markers, in the
message or in a comment: the report ties commits to rounds.

### Baseline rounds

`CLAUDE.md` wants a characterization fixture committed before the change it
guards. A **baseline** item is a test that must **pass** on the untouched
code: that fixture, or a behaviour that already holds and is untested.
Failing means you misread the current behaviour: discard it and write it
again, a failed attempt.

A test that has never failed is evidence of nothing, so a baseline is
committed only once it has been seen failing:

- a gold file the next round moves is proven by that round — name it in the
  test list;
- anything else is proven by hand: stage the test (`git add -A`), break the
  behaviour in the production code, run the gate — it must fail on this test
  only — then `git restore . && git clean -fd` brings the untouched code back
  under the staged test. Keep the failure's key line for the report.

No break makes it fail → it tests nothing: discard it, and say so in the
report. Otherwise commit it alone: `git add -A && git commit -m "<what it pins>"`.

### REFACTOR — optional, folded into the round

Clean up production or test code, assertions and gold files excepted. Gate
green → fold it into the round's commit with `git add -A && git commit --amend
--no-edit`. Red → `git reset --hard HEAD && git clean -fd`: the refactor wasn't
safe, and the round's commit stands as it was.

## When to stop

End the batch — and go straight to the report — when:

- **the budget is spent**;
- **the test list is done** (never invent behaviours to use up the budget);
- **you are stuck**: three failed attempts in a row within one round. Abandon
  the round — `git reset --hard HEAD && git clean -fd`, back to the last
  round's commit or the base — and report what you tried, with the test you
  were trying to pass. Being stuck is a signal for the human, not something to
  push through;
- **a decision is needed** that the test list didn't settle — a syntax
  question `bsc` cannot answer, wording the user will see (an inspection
  message, an intention's name), a new dependency or platform bump, a change
  of scope.

The test list may grow while you work: append a newly discovered behaviour at
the end, never reorder or silently drop items, and mention it in the report.

## The report

Before reporting, run the gate once more, then `./gradlew --no-daemon
verifyPluginProjectConfiguration`. Report the result verbatim — do not fix a
failure found here, it is for the human.

```
## TCR batch on <branch>

Base: <base sha>

| # | commit | behaviour | red failure | failed attempts |
|---|--------|-----------|-------------|-----------------|
| 1 | abc1234 | …        | <key line, or the baseline's proof: "round n" or the break> | 0 |

Stopped because: <budget spent | test list done | stuck on round k | decision: …>
Abandoned: <round, what was tried, the test itself — or "nothing">
Other gold files moved: <round: files — or "none">
Final gate: <pass | failure verbatim>
Test list left: <remaining behaviours>

Review:  git log --reverse -p <base>..HEAD
```

Then tell the human how to keep a prefix: `git reset --hard <the commit of the
last good round>`. Every commit passes its tests, so any of them is a safe
place to stop. Dropping a round from the middle usually conflicts, because
later rounds build on it. Never run either yourself.

## Rules

- NEVER start without a clean tree, a green baseline and an approved test list
- NEVER commit a state that fails the gate — every commit is one working behaviour
- NEVER commit on master, push, merge, or touch a commit from before the base
- NEVER fix a failed attempt — revert it, then try smaller
- NEVER touch `src/test/` in GREEN
- NEVER weaken or delete a test to get to green
- NEVER let the test framework write a gold file
- NEVER encode a syntax assumption `bsc` has not confirmed
- NEVER exceed the budget or pad the test list to fill it
- NEVER stash, reset or clean anything you did not create in this batch
