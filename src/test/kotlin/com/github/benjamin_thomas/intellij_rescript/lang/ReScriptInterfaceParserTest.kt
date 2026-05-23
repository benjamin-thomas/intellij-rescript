package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptModuleTypeDeclaration
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptInterfaceParserTest : BasePlatformTestCase() {

    fun testAbstractModuleTypeDeclaration() {
        // Arrange
        val file = myFixture.configureByText("Test.resi", "module type Counter")

        // Act
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        val decl = PsiTreeUtil.findChildOfType(file, ReScriptModuleTypeDeclaration::class.java)
            ?: error("No ModuleTypeDeclaration found")

        // Assert
        assertEmpty(errors)
        assertEquals("Counter", (decl as PsiNameIdentifierOwner).name)
    }
}
