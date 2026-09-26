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

    fun testInsertsClosingTagForModulePathTag() {
        assertEquals("</Mod.sub>", closingFor("let x = <Mod.sub>"))
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

    fun testNoInsertWhenNestedSameNameTagsAreAllClosed() {
        assertNull(closingForMarked("let x = <div><div|></div></div>"))
    }

    fun testInsertsClosingTagInsideSameNameParent() {
        assertEquals(
            "</div>",
            closingForMarked(
                """
                let x =
                  <div>
                    <div|>
                  </div>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagInsideSameNameComponent() {
        assertEquals(
            "</Row>",
            closingForMarked(
                """
                let x =
                  <Row>
                    <Row|>
                  </Row>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagBeforeSameNameSibling() {
        assertEquals(
            "</li>",
            closingForMarked(
                """
                let x =
                  <ul>
                    <li|>
                    <li>{React.string("a")}</li>
                  </ul>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagInsideGrandparent() {
        // Each level took its parent's closing tag, so only the root lacks one
        assertEquals(
            "</li>",
            closingForMarked(
                """
                let x =
                  <div>
                    <ul>
                      <li|>
                    </ul>
                  </div>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagInsideFragment() {
        assertEquals(
            "</p>",
            closingForMarked(
                """
                let x =
                  <>
                    <p|>
                  </>
                """.trimIndent()
            )
        )
    }

    fun testInsertsFragmentClosingInsideFragment() {
        assertEquals(
            "</>",
            closingForMarked(
                """
                let x =
                  <>
                    <|>
                  </>
                """.trimIndent()
            )
        )
    }

    fun testNoInsertWhenRetypingClosedTagInsideUnclosedParent() {
        // `</span>` cannot be the parent's, so it is the span's own
        assertNull(
            closingForMarked(
                """
                let x =
                  <section>
                    <span|></span>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagInsideBracedChild() {
        assertEquals("</span>", closingForMarked("let x = <div>{<span|>}</div>"))
    }

    fun testInsertsClosingTagInsideRenderProp() {
        assertEquals("</div>", closingForMarked("let x = <Foo render={x => <div|>} />"))
    }
}
