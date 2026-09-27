---
name: local-worker
description: Take one dispatched unit of work from its brief to a merge-ready branch — TDD under local-tdd-tcr for the round budget, then a defect review by the reviewer agent in the next tab — and hand off with a git note. The lead agent of a session that `manage/agents dispatch` starts; not for interactive use.
---

# Worker

One unit, one branch, one worktree. Once you hand your branch off ready, the
orchestrator merges it; the human reviews each of your commits afterwards.
What you hand over lives in git: your commits, and a note on your branch's tip.

## Where you are

- A sandbox whose project is your worktree, on branch `<subject>--<unit>`,
  forked from the parent branch your prompt names. `.git` is writable.
- Your prompt gives the unit, the round budget, the mode and your brief.
  **Confirm** mode: the human approves your test list. **Autonomous** mode:
  the dispatch is the approval.
- The `reviewer` tab, next to yours, runs the agent the human picked to
  review your work.
- You cannot reach the orchestrator or the other workers: they read your
  branch and its note.

## Grant

Commits on your branch only — the rounds `local-tdd-tcr` allows — and your
handoff note. Never another branch, never a merge, never a push.

## 1. Check the brief

The brief says what to achieve, and shows examples with what `bsc` and the
plugin did when it was written. It is evidence, not a plan: how to get there
is yours to find.

- Save it as `tmp/agents/brief.md`: the reviewer reads it there.
- Re-run its examples on your branch. Do they still behave as it says? Is part
  of it already done? Check every syntax claim with `bsc`, as `CLAUDE.md` says.
- A brief that no longer holds — already done, wrong, or too big for your
  budget — is handed off blocked (step 4), with what you found.

## 2. The batch

`local-tdd-tcr` with the given budget. Its test list starts from the brief's
examples: a RED test for each one that fails on your branch. One that already
holds is a baseline only if its test can be proven to fail (TCR's "Baseline
rounds"); otherwise note it as covered. In confirm mode, ask **"Start? (y/N)"** as TCR
does, and the human answers here; in autonomous mode, don't ask. There
is no ticket, so TCR's ticket step does not apply.

Keep TCR's report: it goes into your handoff note. Anything you find out of
scope goes there too, under `Discovered:`, never into this branch.

## 3. Review

```bash
herdr agent prompt reviewer "Use the local-defect-review skill (.agents/skills/local-defect-review/SKILL.md). Base: <parent>. Brief: tmp/agents/brief.md. Write your verdict to tmp/agents/review.md." --wait --timeout 540000
```

A review can outlast the timeout: then run
`herdr agent wait reviewer --timeout 540000` until it returns idle or done.
A plain `wait` right after the prompt would return before the reviewer even
starts, which is why the prompt waits itself. If the reviewer is blocked on a
dialog, that is the human's: hand off blocked.

- **`verdict: pass`** — hand off ready.
- **`verdict: fail`** — each defect's reproduction becomes a RED test: fix
  them in TCR rounds, with what is left of the budget, then ask for one more
  review. The notes are not yours to act on: they go to the human in your
  handoff.
- **A second fail, or the budget spent** — hand off blocked.

## 4. Hand off

With everything committed, write `tmp/agents/handoff.md`:

```
ready

<what the branch does, in two or three lines>

Rounds: <used>/<budget>, <reverted attempts> reverted
Review: <pass, or the defects left>
Reviewer notes: <verbatim, or "none">
Discovered: <out of scope, or "nothing">
Learned: <what you had to work out that is written nowhere, or "nothing">
Contortions: <where you patched around the code rather than through it, or "nothing">
Confusions: <anything in the brief or the skills that was unclear, or "nothing">
```

or, instead of `ready`, `blocked: <one sentence: what is missing, why it
blocks, the exact action the human must take>`. Then attach it to your tip:

```bash
git notes --ref="agents/$(git branch --show-current)" add -f -F tmp/agents/handoff.md HEAD
```

Blocked covers TCR's stops too: stuck, a decision, a new dependency, the
budget spent before the test list. A unit that turns out void — its brief
already holds, or the human drops it in your window — hands off `ready` with
no commits and says so: the orchestrator retires it.

Then say here only what you did and what blocks you — no "next
steps". Stay idle: the human may answer you here. If they do, carry on. Your
next commit leaves the note behind on an older commit, and you hand off again
when you are done.

## Rules

- NEVER work on anything but your unit
- NEVER commit outside your branch, merge, or push
- NEVER change code outside a TCR round — every commit passes the gate
- NEVER act on the reviewer's notes — only its reproduced defects
- NEVER hand off with uncommitted changes
- NEVER answer a dialog in the reviewer's tab — that is the human's
