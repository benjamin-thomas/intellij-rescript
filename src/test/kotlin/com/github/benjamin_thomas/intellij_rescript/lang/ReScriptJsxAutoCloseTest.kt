package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.ReScriptFileType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Characterizes [jsxAutoCloseText] against the behavior of the fixture-based
 * wiring test (ReScriptJsxTypedHandlerTest): the last character of the input
 * plays the role of the just-typed `>`.
 */
class ReScriptJsxAutoCloseTest : BasePlatformTestCase() {

    private fun closingFor(text: String): String? {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("Test.res", ReScriptFileType, text)
        val gt: PsiElement = file.findElementAt(text.length - 1)!!
        return jsxAutoCloseText(gt)
    }

    fun testNoInsertWhenElementAlreadyHasClosingTag() {
        // Retyped the opening tag's `>`; the `|` marks the just-typed char
        assertNull(closingForMarked("let x = <div|>child</div>"))
    }

    private fun closingForMarked(marked: String): String? {
        val text = marked.replace("|", "")
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("Test.res", ReScriptFileType, text)
        val gt = file.findElementAt(marked.indexOf('|'))!!
        return jsxAutoCloseText(gt)
    }

    fun testInsertsClosingTagForElement() {
        assertEquals("</div>", closingFor("let x = <div>"))
    }

    fun testNoInsertWhenTypedGtEndsClosingTag() {
        assertNull(closingFor("let x = <div></div>"))
    }

    fun testInsertsClosingTagForFragment() {
        assertEquals("</>", closingFor("let x = <>"))
    }

    fun testNoInsertWhenTypedGtEndsFragment() {
        assertNull(closingFor("let x = <></>"))
    }

    fun testNoInsertForComparisonGt() {
        assertNull(closingFor("let x = a >"))
    }
}
