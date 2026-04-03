package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptBreadcrumbsProviderTest : BasePlatformTestCase() {

    private val provider = ReScriptBreadcrumbsProvider()

    private fun collectBreadcrumbs(code: String): List<String> {
        val file = myFixture.configureByText("Test.res", code)
        val breadcrumbs = mutableListOf<String>()
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (provider.acceptElement(element)) {
                    breadcrumbs.add(provider.getElementInfo(element))
                }
                super.visitElement(element)
            }
        })
        return breadcrumbs
    }

    fun testLetBindingBreadcrumb() {
        // Act
        val breadcrumbs = collectBreadcrumbs("let x = 1")

        // Assert
        assertEquals(listOf("let x"), breadcrumbs)
    }

    fun testModuleBindingBreadcrumb() {
        // Act
        val breadcrumbs = collectBreadcrumbs("module Foo = { let x = 1 }")

        // Assert
        assertEquals(listOf("module Foo", "let x"), breadcrumbs)
    }

    fun testDestructuringSkipped() {
        // Act
        val breadcrumbs = collectBreadcrumbs("let (a, b) = tuple")

        // Assert — destructuring has no single name, should not appear
        assertEquals(emptyList<String>(), breadcrumbs)
    }

    fun testDecoratedLetBreadcrumb() {
        // Act
        val breadcrumbs = collectBreadcrumbs(
            """
            @react.component
            let make = () => {
              <div />
            }
            """.trimIndent()
        )

        // Assert — shows the let binding, not the decorator
        assertEquals(listOf("let make"), breadcrumbs)
    }

    fun testTypeDeclarationBreadcrumb() {
        // Act
        val breadcrumbs = collectBreadcrumbs("type color = Red | Green | Blue")

        // Assert
        assertEquals(listOf("type color"), breadcrumbs)
    }
}
