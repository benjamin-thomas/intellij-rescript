package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptModuleBinding
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptModuleBindingNameTest : BasePlatformTestCase() {

    private fun findFirstModuleBinding(code: String): ReScriptModuleBinding {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptModuleBinding::class.java)
            ?: error("No ModuleBinding found in: $code")
    }

    fun testSimpleModule() {
        // Act
        val binding = findFirstModuleBinding("module Foo = { let x = 1 }")

        // Assert
        assertInstanceOf(binding, PsiNameIdentifierOwner::class.java)
        assertEquals("Foo", binding.name)
    }

    fun testModuleAlias() {
        // Act
        val binding = findFirstModuleBinding("module Foo = Bar")

        // Assert
        assertEquals("Foo", binding.name)
    }
}
