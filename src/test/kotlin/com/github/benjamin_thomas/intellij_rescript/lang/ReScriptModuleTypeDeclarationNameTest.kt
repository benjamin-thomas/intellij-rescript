package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptModuleTypeDeclaration
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptModuleTypeDeclarationNameTest : BasePlatformTestCase() {

    private fun findFirstModuleTypeDeclaration(code: String): ReScriptModuleTypeDeclaration {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptModuleTypeDeclaration::class.java)
            ?: error("No ModuleTypeDeclaration found in: $code")
    }

    fun testModuleTypeName() {
        // Act
        val decl = findFirstModuleTypeDeclaration("module type Counter = { type t }")

        // Assert
        assertTrue(decl is PsiNameIdentifierOwner)
        val owner = decl as PsiNameIdentifierOwner
        assertEquals("Counter", owner.name)
        assertEquals("Counter", owner.nameIdentifier?.text)
    }
}
