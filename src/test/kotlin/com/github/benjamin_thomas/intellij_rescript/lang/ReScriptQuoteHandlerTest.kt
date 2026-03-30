package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptQuoteHandlerTest : BasePlatformTestCase() {

    fun testDoubleQuoteAutoClose() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('"')
        myFixture.checkResult("let x = \"<caret>\"")
    }

    fun testDoubleQuoteRoundTrip() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('"')
        myFixture.checkResult("let x = \"<caret>\"")
        myFixture.type('"')
        myFixture.checkResult("let x = \"\"<caret>")
    }

    fun testDoubleQuoteOnce() {
        myFixture.configureByText("Test.res", "let x = \"<caret>")
        myFixture.type('"')
        myFixture.checkResult("let x = \"\"<caret>")
    }

    fun testBacktickOnce() {
        myFixture.configureByText("Test.res", "let x = `<caret>")
        myFixture.type('`')
        myFixture.checkResult("let x = ``<caret>")
    }

    fun testBacktickAutoClose() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('`')
        myFixture.checkResult("let x = `<caret>`")
    }

    fun testBacktickRoundTrip() {
        myFixture.configureByText("Test.res", "let x = <caret>")
        myFixture.type('`')
        myFixture.checkResult("let x = `<caret>`")
        myFixture.type('`')
        myFixture.checkResult("let x = ``<caret>")
    }
}
