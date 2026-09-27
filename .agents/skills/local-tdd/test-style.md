# Writing tests in intellij-rescript

How tests are written, whichever loop drives them — `local-tdd` step by step,
or `local-tdd-tcr` for a budget of rounds.

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
  essentially "useless". Flag it to the human rather than keep it: most times,
  deleting the test is the right call.

## Fixtures move `RestartStates.out`

`ReScriptLexerRestartStateCharacterizationTest` lexes every `.res` file under
`src/test/resources/com/github/benjamin_thomas/intellij_rescript/` — parser,
folding, annotator and lexer fixtures alike — into
`lexer/fixtures/RestartStates.out`, one line per token:
`<fixture> <offset> <token> 0x<state> <exact>`. A new fixture moves that gold
in the same step as its own: derive its lines by hand, from a fixture that
already lexes the same way, never from the test's output.
