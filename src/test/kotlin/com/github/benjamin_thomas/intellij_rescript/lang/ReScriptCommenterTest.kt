package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptCommenterTest : BasePlatformTestCase() {

    fun testLineCommentRoundTrip() {
        myFixture.configureByText("Test.res", "let x = 1<caret>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("// let x = 1")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("let x = 1")
    }

    fun testBlockCommentRoundTrip() {
        myFixture.configureByText("Test.res", "<selection>let x = 1</selection>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        myFixture.checkResult("/* \nlet x = 1 */\n")

        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        myFixture.checkResult("let x = 1 \n")
    }
}
