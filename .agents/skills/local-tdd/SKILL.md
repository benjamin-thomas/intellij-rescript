---
name: local-tdd
description: Human-driven strict RED-GREEN-REFACTOR TDD workflow for the intellij-rescript plugin.
---

You are in **TDD (Test-Driven Development)** mode now.

Follow strict RED-GREEN-REFACTOR discipline with user checkpoints expressed
below. For a budget of rounds run without checkpoints, see `local-tdd-tcr`.

## General principles

Write tests as `test-style.md` (next to this file) says — read it before the
first test.

There is no watcher: Gradle compiles on demand. Always pass `--no-daemon`, and
run one class or one method — platform tests boot a headless IDE, so the full
suite is slow.

```
./gradlew --no-daemon testClasses                              # compile only (ABC)
./gradlew --no-daemon test --tests '*ReScriptFooTest'          # one class
./gradlew --no-daemon test --tests '*ReScriptFooTest.testBar'  # one method
```

Make sure to compile the code yourself before running the test then reporting
at each checkpoint.

A red step is _not_ a compiler error. A red step should represent a logical
error, so stub whatever needs to be as needed.

## Commits: proposed at every green, made only on request

**Never stage on your own.** The user stages chunk by chunk to validate the
code — the index is their review tool, not yours.

Before the first test, check once whether `.git` is writable, and whether the
tree is clean:

```bash
test -w "$(git rev-parse --absolute-git-dir)" && echo writable || echo read-only
git status --porcelain
```

Say which mode you are in. If the tree is not clean, say so before starting: a
commit made from this loop takes everything in it.

At every GREEN checkpoint, propose a commit message in the imperative, saying
what the commit does: a title of 50 characters or fewer, a body only when the
title cannot say it all. It covers **everything not committed yet** — read it from
`git diff HEAD` at that moment, never from memory. That is what lets the user
commit by hand, or let several steps pile up, without telling you: the next
proposal simply starts from wherever HEAD is by then.

On `c`, stage everything and commit:

```bash
git add -A && git commit -m "<message>"
```

## The TDD loop

### Phase 1: RED — Write a Failing Test

1. **Understand the requirement** — ask clarifying questions if needed
2. **Write one test** that describes the expected behavior
3. **Compile, then run.** Capture the failure for later user reporting
4. If the test passes at this stage, STOP and investigate why, then inform the
   user before moving forwards.
5. **Report:**

   ```
   ## RED Phase Complete
   - Test file: [path]
   - Test name: [description]
   - Failure: [the assertion failure, verbatim]
   - Planned implementation: [brief description]

   Continue? (y/N)
   ```

6. **STOP and wait for user approval.**

### Phase 2: GREEN — Make It Pass

1. Write the **minimal code** to make the test pass
2. **Compile, then run** — it should now pass
3. **Report:**

   ```
   ## GREEN Phase Complete
   - Implementation: [path]
   - Test status: PASSING

   **Next test intention**: [what the next RED test will verify]
   - [Setup step]
   - [Assertion to verify]

   Uncommitted: [n] step(s) — proposed message: "[message]"

     c  commit, then write the next test   (c <message> to reword it)
     s  skip the commit, write the next test
   ```

   Read-only `.git`: drop the `c` line; the message is for the user to use.

4. **STOP and wait for the answer.** Anything but `c` or `s` — a question, a
   refactor to do first — is handled as it comes; then offer the choice again.

### Phase 3: REFACTOR (optional)

Clean up while keeping tests green. Recompile and rerun after each change.

## Rules

- NEVER write implementation code before a failing test
- NEVER write more than one test at a time
- NEVER move past a checkpoint without the go-ahead — "y" at RED, "c" or "s" at
  GREEN; any other reply is handled first, then the checkpoint is asked again
- NEVER weaken or delete an existing test to get to green. If a test blocks you, stop and inform the human.
- Keep implementations MINIMAL — just enough to pass
- One behavior per test cycle
- NEVER stage on your own — the index is the user's review tool
- NEVER commit except on "c": then stage everything and commit
