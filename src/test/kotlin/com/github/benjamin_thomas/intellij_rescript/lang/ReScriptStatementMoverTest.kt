package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptStatementMoverTest : BasePlatformTestCase() {

    fun testMoveLetUpPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1
            let b<caret> = 2

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let b = 2
            let a = 1

            """.trimIndent()
        )
    }

    fun testMoveMultiLineLetDown() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            let a<caret> = {
              let inner = 1
              inner + 1
            }
            let b = 2

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let b = 2
            let a = {
              let inner = 1
              inner + 1
            }

            """.trimIndent()
        )
    }

    fun testMoveModuleDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            module M<caret> = {
              let x = 1
            }
            let a = {
              let y = 2
              y + 1
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = {
              let y = 2
              y + 1
            }
            module M = {
              let x = 1
            }

            """.trimIndent()
        )
    }

    fun testMoveTypeDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            type color<caret> =
              | Red
              | Green
              | Blue
            let a = {
              let x = 1
              x + 1
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = {
              let x = 1
              x + 1
            }
            type color =
              | Red
              | Green
              | Blue

            """.trimIndent()
        )
    }

    fun testMoveOpenDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            open<caret> Belt
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            open Belt

            """.trimIndent()
        )
    }

    fun testMoveIncludeDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            include<caret> Belt
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            include Belt

            """.trimIndent()
        )
    }

    fun testMoveExternalDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            external<caret> createElement:
              string => React.element = "createElement"
            let a = {
              let x = 1
              x + 1
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = {
              let x = 1
              x + 1
            }
            external createElement:
              string => React.element = "createElement"

            """.trimIndent()
        )
    }

    fun testMoveExceptionDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            exception<caret> NotFound
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            exception NotFound

            """.trimIndent()
        )
    }

    fun testMoveExtensionPointDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            %%raw<caret>(`
              console.log("hello")
            `)
            let a = {
              let x = 1
              x + 1
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = {
              let x = 1
              x + 1
            }
            %%raw(`
              console.log("hello")
            `)

            """.trimIndent()
        )
    }

    fun testMoveDecoratedLetDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            @react.component<caret>
            let make = () => {
              <div />
            }
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            @react.component
            let make = () => {
              <div />
            }

            """.trimIndent()
        )
    }

    fun testMoveDecoratedLetDownWithCursorOnLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            @react.component
            let make<caret> = () => {
              <div />
            }
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            @react.component
            let make = () => {
              <div />
            }

            """.trimIndent()
        )
    }

    fun testMoveLetDownPastDecoratedLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            let a<caret> = 1
            @react.component
            let make = () => {
              <div />
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            @react.component
            let make = () => {
              <div />
            }
            let a = 1

            """.trimIndent()
        )
    }

    fun testMoveDecoratedLetUpPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1
            @react.component<caret>
            let make = () => {
              <div />
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert
        myFixture.checkResult(
            """
            @react.component
            let make = () => {
              <div />
            }
            let a = 1

            """.trimIndent()
        )
    }

    fun testMoveStackedDecoratorsDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            @react.component
            @genType<caret>
            let make = () => {
              <div />
            }
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            @react.component
            @genType
            let make = () => {
              <div />
            }

            """.trimIndent()
        )
    }

    fun testMoveDecoratedTypeDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            @unboxed<caret>
            type a = Name(string)
            let x = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let x = 1
            @unboxed
            type a = Name(string)

            """.trimIndent()
        )
    }

    fun testMoveTopLevelExprDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            Console.log<caret>("hi")
            let a = 1

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let a = 1
            Console.log("hi")

            """.trimIndent()
        )
    }

    fun testMoveLetDownPastLet() {
        // Arrange
        myFixture.configureByText(
            "Test.res",
            """
            let a<caret> = 1
            let b = 2

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert
        myFixture.checkResult(
            """
            let b = 2
            let a = 1

            """.trimIndent()
        )
    }

    fun testMoveLetWithJsxBodyUpWithCursorOnLet() {
        // Arrange: caret on the declaration line of a multi-line JSX component
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1

            let make<caret> = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")} </p>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert: the whole binding moved
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")} </p>
              </div>

            let a = 1

            """.trimIndent()
        )
    }

    fun testMoveJsxChildUpPastSibling() {
        // Arrange: caret on the second child of a multi-line element
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1

            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")}<caret> </p>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert: the children swapped, the bindings stayed
        myFixture.checkResult(
            """
            let a = 1

            let make = () =>
              <div>
                <p> {React.string("2")} </p>
                <p> {React.string("1")} </p>
              </div>

            """.trimIndent()
        )
    }

    fun testMoveJsxChildUpInOnlyItemOfModule() {
        // Arrange: the component is the only item of its module
        myFixture.configureByText(
            "Test.res",
            """
            module A = {
              @react.component
              let make = () =>
                <div>
                  <p> {React.string("1")} </p>
                  <p> {React.string("2")}<caret> </p>
                </div>
            }

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert: the children swapped
        myFixture.checkResult(
            """
            module A = {
              @react.component
              let make = () =>
                <div>
                  <p> {React.string("2")} </p>
                  <p> {React.string("1")} </p>
                </div>
            }

            """.trimIndent()
        )
    }

    fun testMoveJsxChildDownPastSibling() {
        // Arrange: caret on the first child
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                <p> {React.string("1")}<caret> </p>
                <p> {React.string("2")} </p>
              </div>

            let b = 2

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: the children swapped, the bindings stayed
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <p> {React.string("2")} </p>
                <p> {React.string("1")} </p>
              </div>

            let b = 2

            """.trimIndent()
        )
    }

    fun testMoveMultiLineJsxChildDownAsOneUnit() {
        // Arrange: caret on the opening line of a multi-line child
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                <p><caret>
                  {React.string("1")}
                </p>
                <span> {React.string("2")} </span>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: the whole child moved below its sibling
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <span> {React.string("2")} </span>
                <p>
                  {React.string("1")}
                </p>
              </div>

            """.trimIndent()
        )
    }

    fun testMoveFirstJsxChildUpStaysInParent() {
        // Arrange: caret on the first child, a binding above the component
        myFixture.configureByText(
            "Test.res",
            """
            let a = 1

            let make = () =>
              <div>
                <p> {React.string("1")}<caret> </p>
                <p> {React.string("2")} </p>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)

        // Assert: neither the child nor the component moved
        myFixture.checkResult(
            """
            let a = 1

            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")} </p>
              </div>

            """.trimIndent()
        )
    }

    fun testMoveLastJsxChildDownStaysInParent() {
        // Arrange: caret on the last child, a binding below the component
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")}<caret> </p>
              </div>

            let b = 2

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: neither the child nor the component moved
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                <p> {React.string("2")} </p>
              </div>

            let b = 2

            """.trimIndent()
        )
    }

    fun testMoveJsxChildDownPastBracedChild() {
        // Arrange: the next sibling is a braced child
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                <p> {React.string("1")}<caret> </p>
                {React.string("x")}
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: the element moved below the braced child
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                {React.string("x")}
                <p> {React.string("1")} </p>
              </div>

            """.trimIndent()
        )
    }

    fun testMoveBracedJsxChildDownPastSibling() {
        // Arrange: caret on a braced child
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                {React.string("x")}<caret>
                <p> {React.string("1")} </p>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: the braced child moved below the element
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                {React.string("x")}
              </div>

            """.trimIndent()
        )
    }

    fun testMoveMultiLineBracedJsxChildDownAsOneUnit() {
        // Arrange: caret inside a multi-line braced child
        myFixture.configureByText(
            "Test.res",
            """
            let make = () =>
              <div>
                {
                  React.string("x")<caret>
                }
                <p> {React.string("1")} </p>
              </div>

            """.trimIndent()
        )

        // Act
        myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)

        // Assert: the whole braced child moved below the element
        myFixture.checkResult(
            """
            let make = () =>
              <div>
                <p> {React.string("1")} </p>
                {
                  React.string("x")
                }
              </div>

            """.trimIndent()
        )
    }
}
