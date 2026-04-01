package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptBraceMatcherTest : BasePlatformTestCase() {

    fun testCurlyBraces() {
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
