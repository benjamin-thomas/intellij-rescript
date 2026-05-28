package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptAndTypeDeclaration
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptTypeDeclaration
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptTypeDeclarationNameTest : BasePlatformTestCase() {

    private fun findFirstTypeDeclaration(code: String): ReScriptTypeDeclaration {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptTypeDeclaration::class.java)
            ?: error("No TypeDeclaration found in: $code")
    }

    private fun findFirstAndTypeDeclaration(code: String): ReScriptAndTypeDeclaration {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptAndTypeDeclaration::class.java)
            ?: error("No AndTypeDeclaration found in: $code")
    }

    fun testSimpleType() {
        // Act
        val decl = findFirstTypeDeclaration("type color = Red | Green | Blue")

        // Assert
        assertInstanceOf(decl, PsiNameIdentifierOwner::class.java)
        assertEquals("color", decl.name)
    }

    fun testTypeWithParams() {
        // Act
        val decl = findFirstTypeDeclaration("type option<'a> = None | Some('a)")

        // Assert
        assertEquals("option", decl.name)
    }

    fun testAbstractType() {
        // Act
        val decl = findFirstTypeDeclaration("type t")

        // Assert
        assertEquals("t", decl.name)
    }

    fun testMutuallyRecursiveContinuation() {
        // Act — the `and b = …` continuation is its own named PSI node
        val decl = findFirstAndTypeDeclaration("type a = int\nand b = string")

        // Assert
        assertInstanceOf(decl, PsiNameIdentifierOwner::class.java)
        assertEquals("b", decl.name)
    }

    fun testDecoratedContinuation() {
        // Act — a leading decorator must not shift the name off the LIDENT
        val decl = findFirstAndTypeDeclaration("type a = int\n@live\nand b = string")

        // Assert
        assertEquals("b", decl.name)
    }
}
