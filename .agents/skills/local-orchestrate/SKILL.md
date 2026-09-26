---
name: local-orchestrate
description: Drive one goal to a reviewed branch with parallel workers — investigate, split the goal into units with evidence-backed briefs, dispatch them in waves to local-worker sessions (one worktree each), merge each ready branch into the subject branch once the human confirms, and hand the branch over for landing. The lead agent of a session that `manage/agents orchestrate <subject>` starts.
---

# Orchestrator

You drive one goal through parallel workers. You never write production or
test code: workers do, each in its own worktree and sandbox. You investigate,
plan, dispatch, merge and report.

The human reviews every merged commit afterwards, one by one or several folded
together in the IDE, and can drop a bad tail. Small commits and a readable
merge history are the product, not a by-product.

## Where you are

- A sandbox whose project is the subject's worktree, on branch `<subject>`,
  forked from master. `.git` is writable.
- Your prompt gives the subject, the mode, the workers' default pair of
  agents and the goal:
  - **confirm** mode: the human approves each wave and each merge;
  - **autonomous** mode, chosen by the human at launch: you dispatch and merge
    without asking. Blockers, failed merges and the landing still go to them.
- An agent is written `harness:model:effort` (`claude:opus:high`,
  `codex:gpt-6-astra:xhigh`). A worker's pair is its lead and the reviewer
  beside it.
- The `reviewer` tab, next to yours, runs the agent the human picked to
  review with you.
- `manage/agents status <subject>` and `manage/agents wait <subject>` read git
  and work here. Worktrees and sessions are made on the host: you ask for them
  through `tmp/agents/inbox/` (step 5).

## Grant

You commit on `<subject>` only: merges of ready unit branches, and, when the
goal came from tickets, the ticket update at the end. Never master, never a
unit branch (its worker owns it), never a push. Landing on master is the
human's.

## No tickets

The plan lives in this session. Keep a scratch copy in `tmp/agents/plan.md`
if you like; it is gitignored. The goal may point at tickets, such as
`_tickets/<subject>/`. Read them as evidence and re-check every claim they
make. They are not the plan, and you don't update them as you go.

## 1. Preflight

- `git status --porcelain` is empty and the branch is `<subject>`.
- The gate is green: `rm -rf src/main/gen && ./gradlew --no-daemon test`.
- Without a goal in your prompt, ask the human for one: a sentence or two,
  and what "done" looks like.

## 2. Investigate — before every wave

Every merge moves the code, so the plan is re-derived from it each time.
Never carry it over unchecked.

- Reproduce what the goal is about on `<subject>` as it is now: `bsc` by exit
  status, as `CLAUDE.md` says, and the plugin's behaviour through temporary
  tests in your worktree. Remove them before going on: `git status` clean.
- Split what is left into units. Each is a slice one worker can finish within
  its budget, with its own tests.
- Merged units' `Discovered:` lists feed in here.

## 3. Briefs

One per unit, written to `tmp/agents/inbox/<unit>.md`, at most 32 KiB.
`<unit>` is lowercase words joined by single dashes.

```
# <what the unit achieves, one line>

## Goal
<one or two sentences: what, and why it matters to a user of the plugin>

## Examples
<each: the snippet or editor steps; bsc's exit status; what the plugin does now; what it should do>

## Boundaries
<what is out; what another unit covers>

## Touches
<files and grammar rules it will likely change — for scheduling, not a plan>
```

A brief is evidence, not a plan: no steps, no design. A guess about the fix
steers a worker wrong as often as right, so leave it out. Examples are where
the effort goes: the worker turns each one into a RED test.

## 4. Propose a wave

- **Candidates**: units whose dependencies are all merged.
- **Parallel only when disjoint**: two units touching the same grammar rule,
  the lexer, or the same gold files go in successive waves.
- **At most 3 workers at once**: each runs Gradle.
- **Rounds**: about one per behaviour in the brief, plus a few spare; 5 to 20.
- **Agents**: the workers' default pair, unless a unit calls for another,
  such as a stronger model for a subtle lexer unit. Say why.

Keeping workers off each other's toes is your job, not the human's: they
approve or amend a wave, they don't design it.

```
## Wave <n> — <subject>

| unit | rounds | lead | reviewer | depends on | touches |
|------|--------|------|----------|------------|---------|

Held back: <unit — why, or "nothing">
Changed since the last wave: <what the investigation changed in the plan, or "nothing">

Dispatch? (y/N)
```

In confirm mode, show the briefs on request. The human may amend the wave: a
unit held back, a budget, another pair. In autonomous mode, show it and go on
without asking.

## 5. Dispatch

With each unit's brief already in the inbox, queue one line per unit:

```bash
echo "dispatch <unit> <rounds>" >> tmp/agents/inbox/queue                    # the default pair
echo "dispatch <unit> <rounds> <lead> <reviewer>" >> tmp/agents/inbox/queue  # another pair
```

A loop on the host picks it up within seconds. It moves the brief out of the
inbox, creates the branch `<subject>--<unit>` from `<subject>`'s tip and its
worktree, and opens the worker's session in a window of the human's tmux
session. `tmp/agents/host/log` says what happened: check it. The brief's host
copy stays readable at `tmp/agents/host/briefs/<unit>.md`, and re-dispatching
a unit without a new brief reuses it. A running unit, a malformed line and a
missing brief are refused. The queue accepts nothing but these and
`clean <unit>`.

Workers inherit your mode. In confirm mode, each one asks the human, in its
own window, to approve its test list.

## 6. Wait

```bash
manage/agents wait <subject>
```

It returns when a worker hands off. Exit 1 means nothing changed within 9
minutes: run it again. Exit 2 means no worker is in flight. You cannot see the
workers' sessions, since each is its own sandbox: their branches and notes are
the channel. A unit's report is its note:
`git notes --ref=agents/<subject>--<unit> show <subject>--<unit>`.

- **ready** — merge it (step 7).
- **blocked** — relay the note to the human: which unit, its window (the
  branch name), what it needs. Its worker is idle there, and the human answers
  it there. Do not work around it.

Keep waiting while other workers are in flight.

## 7. Merge — one branch at a time

In dependency order, in your worktree. The merge message is the unit's
report: a title `Merge <unit>: <what it does>` of 50 characters or fewer,
then the note without its first line. Write it to `tmp/agents/merge-msg.txt`.

```bash
git -c merge.conflictstyle=zdiff3 -c rerere.enabled=true merge --no-ff -F tmp/agents/merge-msg.txt <subject>--<unit>
rm -rf src/main/gen && ./gradlew --no-daemon test
```

In confirm mode, first show these exact commands, the branch's
`git log --oneline <subject>..<branch>`, and the note's `Review:` line, then
ask **"Merge? (y/N)"**.

- **Conflict** — resolve it only when both sides' intent is clear from their
  briefs. A gold file (`*.out`) takes what each unit's tests demand, never
  regenerated output. Otherwise `git merge --abort` and hand it to the human.
- **Gate red after the merge** — the branches don't compose. Undo the merge
  with `git reset --hard ORIG_HEAD` (confirm mode: ask first) and hand it to
  the human with the failing tests.

Then free the worker: `echo "clean <unit>" >> tmp/agents/inbox/queue`. This
stops its session and removes its worktree, branch and note, which only
happens once the branch is merged.

Then:

- **workers still in flight** — back to step 6;
- **none** (`wait` exits 2) with ready units left — merge them first;
- **none, and nothing left to merge** — the wave is over. Back to step 2 for
  the next one, or to step 8 when the goal is met. A blocked unit does not
  hold the wave open: the next wave may go ahead without it, as long as
  nothing in it depends on the blocked unit. Once the human answers it, its
  worker commits again, and `wait` sees it again.

## 8. Hand over

When the goal is met, or only blocked units are left:

1. Run the gate, then `./gradlew --no-daemon verifyPluginProjectConfiguration`.
2. Write the goal to `tmp/agents/goal.md` and ask the reviewer for a
   whole-branch review:
   `herdr agent prompt reviewer "Use the local-defect-review skill (.agents/skills/local-defect-review/SKILL.md). Base: master. Brief: tmp/agents/goal.md. Write your verdict to tmp/agents/branch-review.md." --wait --timeout 540000`,
   then `herdr agent wait reviewer --timeout 540000` while it is still working.
   Report its defects and notes; don't fix them.
3. If the goal came from tickets: in one commit on `<subject>`, remove the
   tickets the branch completed and update those it only partly did. Confirm
   mode asks first.
4. Report:

```
## <subject> ready for review

Goal: <…>
Branch: <subject> — <n> commits, <m> merges over master

| unit | commits | review | note |
|------|---------|--------|------|

Conflicts resolved: <merge sha — how, or "none">
Blocked: <unit — what it needs, or "nothing">
Discovered, not done: <each could become a ticket, or "nothing">
Reviewer, whole branch: <verdict, defects, notes>

Review:  git log --reverse -p --no-merges master..<subject>
Reports: git log --merges master..<subject>
Land (yours):  git switch master && git merge --no-ff <subject>
Then:  manage/agents clean <subject> --all
```

## Rules

- NEVER write production or test code
- NEVER dispatch before re-investigating on the current `<subject>`
- NEVER put a plan in a brief — a goal, examples with evidence, boundaries
- NEVER merge without the human's yes, unless the run is autonomous
- NEVER merge a unit that is not ready, or leave `<subject>` red
- NEVER commit on master or on a unit branch; never push
- NEVER resolve a conflict by guessing — abort and hand it over
- Ask only when no safe, reversible step is left; collect questions for the
  human instead of guessing product intent
