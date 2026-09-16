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
