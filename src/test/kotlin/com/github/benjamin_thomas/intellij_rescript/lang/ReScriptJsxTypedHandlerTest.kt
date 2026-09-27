package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptJsxTypedHandlerTest : BasePlatformTestCase() {

    fun testGreaterThanClosesTag() {
        // Arrange: caret right after the tag name of an opening tag
        myFixture.configureByText("Test.res", "let x = <div<caret>")
        // Act: type the `>` that completes the opening tag
        myFixture.type('>')
        // Assert: closing tag inserted, caret between the tags
        myFixture.checkResult("let x = <div><caret></div>")
    }

    fun testGreaterThanClosesTagInsideParentElement() {
        // Arrange: a new opening tag inside an element that is already closed
        myFixture.configureByText(
            "Test.res",
            """
            let x =
              <div>
                <span<caret>
              </div>

            """.trimIndent()
        )
        // Act: type the `>` that completes the new opening tag
        myFixture.type('>')
        // Assert: the new tag gets its own closing tag, caret between the tags
        myFixture.checkResult(
            """
            let x =
              <div>
                <span><caret></span>
              </div>

            """.trimIndent()
        )
    }

    fun testGreaterThanOnItsOwnLineClosesTagInsideParentElement() {
        // Arrange: a multi-line opening tag inside a closed element
        myFixture.configureByText(
            "Test.res",
            """
            let x =
              <div>
                <span
                  className="x"
                <caret>
              </div>

            """.trimIndent()
        )
        // Act: type the `>` on its own line
        myFixture.type('>')
        // Assert: the new tag gets its own closing tag, caret between the tags
        myFixture.checkResult(
            """
            let x =
              <div>
                <span
                  className="x"
                ><caret></span>
              </div>

            """.trimIndent()
        )
    }

    fun testGreaterThanBeforeTagsOwnGreaterThanInsertsNoClosingTag() {
        // Arrange: caret just before the `>` of a tag that is already closed
        myFixture.configureByText("Test.res", "let x = <div><div<caret>></div></div>")
        // Act: type a second `>`
        myFixture.type('>')
        // Assert: no closing tag inserted
        myFixture.checkResult("let x = <div><div><caret>></div></div>")
    }

    fun testAutoCloseIsOneUndoStep() {
        // Arrange: type `>` to trigger the auto-close
        myFixture.configureByText("Test.res", "let x = <div<caret>")
        myFixture.type('>')
        // Act: undo
        myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        // Assert: both the `>` and the inserted closing tag go in one step
        myFixture.checkResult("let x = <div<caret>")
    }
}
