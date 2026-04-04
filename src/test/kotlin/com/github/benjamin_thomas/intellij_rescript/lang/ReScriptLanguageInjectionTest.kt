package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptStringLiteral
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptTemplateLiteral
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptLanguageInjectionTest : BasePlatformTestCase() {

    fun testStringLiteralIsInjectionHost() {
        // Arrange
        val file = myFixture.configureByText("Test.res", """let x = "hello"""")
        val literal = PsiTreeUtil.findChildOfType(file, ReScriptStringLiteral::class.java)
            ?: error("No StringLiteral found")

        // Assert
        assertInstanceOf(literal, PsiLanguageInjectionHost::class.java)
        assertTrue((literal as PsiLanguageInjectionHost).isValidHost)
    }

    fun testTemplateLiteralIsInjectionHost() {
        // Arrange
        val file = myFixture.configureByText("Test.res", "let x = `hello`")
        val literal = PsiTreeUtil.findChildOfType(file, ReScriptTemplateLiteral::class.java)
            ?: error("No TemplateLiteral found")

        // Assert
        assertInstanceOf(literal, PsiLanguageInjectionHost::class.java)
        assertTrue((literal as PsiLanguageInjectionHost).isValidHost)
    }

    fun testStringLiteralManipulatorRange() {
        // Arrange
        val file = myFixture.configureByText("Test.res", """let x = "hello"""")
        val literal = PsiTreeUtil.findChildOfType(file, ReScriptStringLiteral::class.java)
            ?: error("No StringLiteral found")

        // Act
        val range = ElementManipulators.getValueTextRange(literal)

        // Assert — range excludes the quotes: "hello" -> hello
        assertEquals(TextRange(1, 6), range)
    }

    fun testTemplateLiteralManipulatorRange() {
        // Arrange
        val file = myFixture.configureByText("Test.res", "let x = `hello`")
        val literal = PsiTreeUtil.findChildOfType(file, ReScriptTemplateLiteral::class.java)
            ?: error("No TemplateLiteral found")

        // Act
        val range = ElementManipulators.getValueTextRange(literal)

        // Assert — range excludes the backticks: `hello` -> hello
        assertEquals(TextRange(1, 6), range)
    }
}
