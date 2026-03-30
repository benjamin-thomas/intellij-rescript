package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptBraceMatcherTest : BasePlatformTestCase() {

    fun testCurlyBraces() {
        myFixture.configureByText("Test.res", "let x = {<caret>}")
        val matched = myFixture.doHighlighting()
        // The brace matcher should highlight the matching pair.
        // Without a matcher registered, typing } won't auto-insert.
        // Test auto-close: type { and get {} with cursor between.
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('{')
        myFixture.checkResult("let x = {<caret>}")
    }

    fun testParentheses() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('(')
        myFixture.checkResult("let x = (<caret>)")
    }

    fun testSquareBrackets() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('[')
        myFixture.checkResult("let x = [<caret>]")
    }
}
