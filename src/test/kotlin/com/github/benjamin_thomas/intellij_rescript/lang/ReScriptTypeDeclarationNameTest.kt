package com.github.benjamin_thomas.intellij_rescript.lang

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
}
