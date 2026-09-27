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

    /**
     * What gets inserted when the last char of [text] is the `>` just typed.
     *
     * Examples:
     * - `closingFor("let x = <div>")` is the user typing `>` at the end of
     *   `let x = <div`. It returns `"</div>"`.
     * - `closingFor("let x = <div></div>")` is the user typing `>` at the end of
     *   `let x = <div></div`. It returns null: that `>` ends a closing tag.
     */
    private fun closingFor(text: String): String? {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText("Test.res", ReScriptFileType, text)
        val gt: PsiElement = file.findElementAt(text.length - 1)!!
        return jsxAutoCloseText(gt)
    }

    fun testNoInsertWhenElementAlreadyHasClosingTag() {
        // Retyped the opening tag's `>`
        assertNull(closingForMarked("let x = <div|>child</div>"))
    }

    /**
     * Like [closingFor], for a `>` typed mid-text. The `|` marks the caret: the
     * char right after it is the one just typed. Every `|` is removed before
     * parsing, so [marked] must not contain another one.
     *
     * Examples:
     * - `closingForMarked("let x = <div><span|></div>")` is the user turning
     *   `let x = <div><span</div>` into `let x = <div><span></div>` by typing
     *   `>` after `<span`. It returns `"</span>"`: the `</div>` is the div's.
     * - `closingForMarked("let x = <div|>child</div>")` is the user turning
     *   `let x = <divchild</div>` into `let x = <div>child</div>` by typing
     *   `>` after `<div`. It returns null: the div already has its closing tag.
     */
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

    fun testInsertsFragmentClosingInsideElement() {
        assertEquals(
            "</>",
            closingForMarked(
                """
                let x = <div>
                  <|>
                </div>
                """.trimIndent()
            )
        )
    }

    fun testInsertsClosingTagInsideDifferentlySpacedPathParent() {
        assertEquals(
            "</span>",
            closingForMarked(
                """
                let x = <Mod . outer>
                  <span|>
                </Mod.outer>
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
