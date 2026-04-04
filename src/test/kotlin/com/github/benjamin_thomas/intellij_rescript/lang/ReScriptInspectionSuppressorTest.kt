package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReScriptInspectionSuppressorTest : BasePlatformTestCase() {

    private val suppressor = ReScriptInspectionSuppressor()

    private fun findDeclaration(code: String): PsiElement {
        val file = myFixture.configureByText("Test.res", code)
        return file.children.first { it.text.contains("let") }
    }

    fun testRecognizesSuppressionComment() {
        // Arrange
        val decl = findDeclaration(
            """
            // noinspection SomeInspection
            let x = "hello"
            """.trimIndent()
        )

        // Act
        val suppressed = suppressor.isSuppressedFor(decl, "SomeInspection")

        // Assert
        assertTrue(suppressed)
    }

    fun testNotSuppressedWithoutComment() {
        // Arrange
        val decl = findDeclaration(
            """
            let x = "hello"
            """.trimIndent()
        )

        // Act
        val suppressed = suppressor.isSuppressedFor(decl, "SomeInspection")

        // Assert
        assertFalse(suppressed)
    }

    fun testProvidesSuppressActions() {
        // Arrange
        val decl = findDeclaration(
            """
            let x = "hello"
            """.trimIndent()
        )

        // Act
        val actions = suppressor.getSuppressActions(decl, "SomeInspection")

        // Assert
        assertTrue("Expected suppress actions to be available", actions.isNotEmpty())
    }

    fun testSuppressActionInsertsCommentAboveDeclaration() {
        // Arrange
        val decl = findDeclaration(
            """
            let x = "hello"
            """.trimIndent()
        )
        val action = suppressor.getSuppressActions(decl, "SomeInspection").first()
        val descriptor = InspectionManager.getInstance(project).createProblemDescriptor(
            decl, "test", null as LocalQuickFix?, ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )

        // Act
        WriteCommandAction.runWriteCommandAction(project) {
            action.applyFix(project, descriptor)
        }

        // Assert
        assertEquals(
            """
            // noinspection SomeInspection
            let x = "hello"
            """.trimIndent(),
            myFixture.file.text
        )
    }
}
