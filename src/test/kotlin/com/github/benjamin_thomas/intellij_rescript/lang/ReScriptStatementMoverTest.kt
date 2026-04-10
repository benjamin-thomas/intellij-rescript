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
}
