package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptAndLetBinding
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptLetBinding
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptLetBindingNameTest : BasePlatformTestCase() {

    private fun findFirstLetBinding(code: String): ReScriptLetBinding {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptLetBinding::class.java)
            ?: error("No LetBinding found in: $code")
    }

    private fun findFirstAndLetBinding(code: String): ReScriptAndLetBinding {
        val file = myFixture.configureByText("Test.res", code)
        return PsiTreeUtil.findChildOfType(file, ReScriptAndLetBinding::class.java)
            ?: error("No AndLetBinding found in: $code")
    }

    fun testSimpleBinding() {
        // Act
        val binding = findFirstLetBinding("let x = 1")

        // Assert
        assertInstanceOf(binding, PsiNameIdentifierOwner::class.java)
        assertEquals("x", binding.name)
    }

    fun testRecBinding() {
        // Act
        val binding = findFirstLetBinding("let rec factorial = (n) => n <= 1 ? 1 : n * factorial(n - 1)")

        // Assert
        assertEquals("factorial", binding.name)
    }

    fun testTupleDestructuring() {
        // Act
        val binding = findFirstLetBinding("let (a, b) = tuple")

        // Assert — destructuring has no single name
        assertNull(binding.name)
    }

    fun testRecordDestructuring() {
        // Act
        val binding = findFirstLetBinding("let {name, age} = person")

        // Assert — destructuring has no single name
        assertNull(binding.name)
    }

    fun testDiscard() {
        // Act
        val binding = findFirstLetBinding("let _ = sideEffect()")

        // Assert — discard has no name
        assertNull(binding.name)
    }

    fun testMutuallyRecursiveContinuation() {
        // Act — the `and g = …` continuation is its own named PSI node
        val binding = findFirstAndLetBinding("let rec f = x => g(x)\nand g = x => f(x)")

        // Assert
        assertInstanceOf(binding, PsiNameIdentifierOwner::class.java)
        assertEquals("g", binding.name)
    }
}
