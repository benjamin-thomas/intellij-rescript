---
name: local-tdd
description: Human-driven strict RED-GREEN-REFACTOR TDD workflow for the intellij-rescript plugin.
---

You are in **TDD (Test-Driven Development)** mode now.

Follow strict RED-GREEN-REFACTOR discipline with user checkpoints expressed
below.

## General principles

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

   Continue? (y/N)
   ```

4. **STOP and wait for user approval.**

### Phase 3: REFACTOR (optional)

Clean up while keeping tests green. Recompile and rerun after each change.

## Rules

- NEVER write implementation code before a failing test
- NEVER write more than one test at a time
- NEVER continue past a checkpoint without user approval — only "y" or "yes" proceeds
- NEVER weaken or delete an existing test to get to green. If a test blocks you, stop and inform the human.
- Keep implementations MINIMAL — just enough to pass
- One behavior per test cycle
- NEVER run `git` write commands — the human stages and commits

## Test Structure (Arrange-Act-Assert)

Use the "AAA comments" (Arrange, Act, Assert), to enforce clear responsibilities in the test code.

Arrange step is optional when testing pure code.

Tests should "read like a story", so a one-line comment describing each section is good here.

### Example

```kotlin
class ReScriptStatementMoverTest : BasePlatformTestCase() {

    fun testMoveLetUpPastLet() {
        // Arrange: two bindings, caret on the second
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1
            let b<caret> = 2

            """.trimIndent()
        )

        // Act: move statement up
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert: the bindings swapped, the caret followed
        myFixture.checkResult(
            """
            let b = 2
            let a = 1

            """.trimIndent()
        )
    }
}
```

## Assertion Practices

- **Hardcode expected values** — don't run functions or extract to var here.
- **Assert only relevant values to the test scenario** – reduce the testing area, tests should be as readable as possible.
- **Fail unreachable branches explicitly** — don't swallow errors. If a condition is impossible, we should assert that.
- **Don't test for the absence of behavior** – generally. If changing the
  "Arrange" block doesn't affect the test's outcome, that's a sign our test is
  essentially "useless". So stop and ask the human what to do when you detect
  such cases. Most times, deleting the test is the right call.
