package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.tree.TokenSet
import kotlin.test.Test

// Kotlin still treats `$` as string-template syntax even in raw strings.
private const val DOLLAR = "$"

class ReScriptLexerTest {
    private fun runLexerTest(inputFile: String, expectedOutputFile: String) =
        runSnapshotTest(ReScriptLexerAdapter(), inputFile, expectedOutputFile)

    @Test
    fun testKeywords() = runLexerTest("Keywords.res", "Keywords.out")

    @Test
    fun testIdentifiers() = runLexerTest("Identifiers.res", "Identifiers.out")

    @Test
    fun testLiterals() = runLexerTest("Literals.res", "Literals.out")

    @Test
    fun testComments() = runLexerTest("Comments.res", "Comments.out")

    @Test
    fun testCommentsNested() = runLexerTest("CommentsNested.res", "CommentsNested.out")

    @Test
    fun testOperators() = runLexerTest("Operators.res", "Operators.out")

    @Test
    fun testOperatorsV12BitwiseAnd() = runLexerTest("OperatorsV12BitwiseAnd.res", "OperatorsV12BitwiseAnd.out")

    @Test
    fun testOperatorsV12BitwiseOr() = runLexerTest("OperatorsV12BitwiseOr.res", "OperatorsV12BitwiseOr.out")

    @Test
    fun testOperatorsV12BitwiseXor() = runLexerTest("OperatorsV12BitwiseXor.res", "OperatorsV12BitwiseXor.out")

    @Test
    fun testOperatorsV12BitwiseNot() = runLexerTest("OperatorsV12BitwiseNot.res", "OperatorsV12BitwiseNot.out")

    @Test
    fun testOperatorsV12ShiftLeft() = runLexerTest("OperatorsV12ShiftLeft.res", "OperatorsV12ShiftLeft.out")

    @Test
    fun testOperatorsV12ShiftRight() = runLexerTest("OperatorsV12ShiftRight.res", "OperatorsV12ShiftRight.out")

    @Test
    fun testOperatorsV12UnsignedShiftRight() = runLexerTest("OperatorsV12UnsignedShiftRight.res", "OperatorsV12UnsignedShiftRight.out")

    @Test
    fun testOperatorsV12Exponentiation() = runLexerTest("OperatorsV12Exponentiation.res", "OperatorsV12Exponentiation.out")

    @Test
    fun testOperatorsV12StrictEqual() = runLexerTest("OperatorsV12StrictEqual.res", "OperatorsV12StrictEqual.out")

    @Test
    fun testOperatorsV12StrictNotEqual() = runLexerTest("OperatorsV12StrictNotEqual.res", "OperatorsV12StrictNotEqual.out")

    @Test
    fun testOperatorsV12Coercion() = runLexerTest("OperatorsV12Coercion.res", "OperatorsV12Coercion.out")

    @Test
    fun testOperatorsV12Range() = runLexerTest("OperatorsV12Range.res", "OperatorsV12Range.out")

    @Test
    fun testComparisonOperators() = runLexerTest("ComparisonOperators.res", "ComparisonOperators.out")

    @Test
    fun testGenericTypeParams() = runLexerTest("GenericTypeParams.res", "GenericTypeParams.out")

    @Test
    fun testJsxSelfClosing() = runLexerTest("JsxSelfClosing.res", "JsxSelfClosing.out")

    @Test
    fun testJsxAttributes() = runLexerTest("JsxAttributes.res", "JsxAttributes.out")

    @Test
    fun testJsxAttributeLiteralValues() =
        runLexerTest("JsxAttributeLiteralValues.res", "JsxAttributeLiteralValues.out")

    // A hyphenated lowercase name is one LIDENT; a stray `-` in a tag is BAD_CHARACTER.
    @Test
    fun testJsxHyphenatedNames() =
        runLexerTest("JsxHyphenatedNames.res", "JsxHyphenatedNames.out")

    @Test
    fun testJsxTagNewlineRescue() =
        runLexerTest("JsxTagNewlineRescue.res", "JsxTagNewlineRescue.out")

    @Test
    fun testJsxTagNewlineRescueFramePop() =
        runLexerTest("JsxTagNewlineRescueFramePop.res", "JsxTagNewlineRescueFramePop.out")

    @Test
    fun testJsxTagNewlineRescueInterpolation() =
        runLexerTest("JsxTagNewlineRescueInterpolation.res", "JsxTagNewlineRescueInterpolation.out")

    @Test
    fun testJsxChildren() = runLexerTest("JsxChildren.res", "JsxChildren.out")

    // Regression: two closing tags on one line used to enter the REGEX state
    // (`/` after `<`) and swallow `/div></s` as a regex literal.
    @Test
    fun testJsxClosingTagsOneLine() = runLexerTest("JsxClosingTagsOneLine.res", "JsxClosingTagsOneLine.out")

    // Fragments are nameless tags: `<>` = JSX_LT + JSX_GT, `</>` = JSX_LT_SLASH + JSX_GT.
    @Test
    fun testJsxFragment() = runLexerTest("JsxFragment.res", "JsxFragment.out")

    @Test
    fun testJsxTemplateInterplay() = runLexerTest("JsxTemplateInterplay.res", "JsxTemplateInterplay.out")

    // Flagship nesting shape: three JSX_CONTENT frames live at once
    // (element > braced expr > element > braced expr > element > braced expr).
    @Test
    fun testJsxNestedBracedElements() = runLexerTest("JsxNestedBracedElements.res", "JsxNestedBracedElements.out")

    // Regression (flunk review): a completed self-closing element is an
    // expression end, so a following `/` is division (not a regex start) and
    // a following `<` is comparison (not another tag).
    @Test
    fun testJsxAfterSelfClosing() = runLexerTest("JsxAfterSelfClosing.res", "JsxAfterSelfClosing.out")

    // Same, for elements completed by a closing tag or fragment close.
    @Test
    fun testJsxAfterClosedElement() = runLexerTest("JsxAfterClosedElement.res", "JsxAfterClosedElement.out")

    @Test
    fun testDelimiters() = runLexerTest("Delimiters.res", "Delimiters.out")

    @Test
    fun testDecorators() = runLexerTest("Decorators.res", "Decorators.out")

    @Test
    fun testSpecial() = runLexerTest("Special.res", "Special.out")

    @Test
    fun testRegex() = runLexerTest("Regex.res", "Regex.out")

    @Test
    fun testNumericHex() = runLexerTest("NumericHex.res", "NumericHex.out")

    @Test
    fun testNumericOctal() = runLexerTest("NumericOctal.res", "NumericOctal.out")

    @Test
    fun testNumericBinary() = runLexerTest("NumericBinary.res", "NumericBinary.out")

    @Test
    fun testNumericUnderscores() = runLexerTest("NumericUnderscores.res", "NumericUnderscores.out")

    @Test
    fun testNumericFloats() = runLexerTest("NumericFloats.res", "NumericFloats.out")

    @Test
    fun testNumericBigInt() = runLexerTest("NumericBigInt.res", "NumericBigInt.out")

    @Test
    fun testStringSimple() = runLexerTest("StringSimple.res", "StringSimple.out")

    @Test
    fun testStringWithEscape() = runLexerTest("StringWithEscape.res", "StringWithEscape.out")

    @Test
    fun testStringWithEscapedQuote() = runLexerTest("StringWithEscapedQuote.res", "StringWithEscapedQuote.out")

    @Test
    fun testStringWithTab() = runLexerTest("StringWithTab.res", "StringWithTab.out")

    @Test
    fun testStringMultiline() = runLexerTest("StringMultiline.res", "StringMultiline.out")

    @Test
    fun testStringEmpty() = runLexerTest("StringEmpty.res", "StringEmpty.out")

    @Test
    fun testStringUnclosed() = runLexerTest("StringUnclosed.res", "StringUnclosed.out")

    @Test
    fun testTemplateSimple() = runLexerTest("TemplateSimple.res", "TemplateSimple.out")

    @Test
    fun testTemplateWithSpaces() = runLexerTest("TemplateWithSpaces.res", "TemplateWithSpaces.out")

    @Test
    fun testTemplateWithEscapedBackticks() =
        runLexerTest("TemplateWithEscapedBackticks.res", "TemplateWithEscapedBackticks.out")

    @Test
    fun testTemplateInterpolationSimple() =
        runLexerTest("TemplateInterpolationSimple.res", "TemplateInterpolationSimple.out")

    @Test
    fun testTemplateInterpolationNestedBraces() =
        runLexerTest("TemplateInterpolationNestedBraces.res", "TemplateInterpolationNestedBraces.out")

    @Test
    fun testTemplateInterpolationStringLiteral() =
        runLexerTest("TemplateInterpolationStringLiteral.res", "TemplateInterpolationStringLiteral.out")

    @Test
    fun testTemplateInterpolationNestedTemplate() =
        runLexerTest("TemplateInterpolationNestedTemplate.res", "TemplateInterpolationNestedTemplate.out")

    @Test
    fun testTemplateEmpty() = runLexerTest("TemplateEmpty.res", "TemplateEmpty.out")

    @Test
    fun testTemplateUnclosed() = runLexerTest("TemplateUnclosed.res", "TemplateUnclosed.out")

    @Test
    fun testZeroStateForKeywordsAndIdentifiers() {
        val tokens = TokenSet.create(
            ReScriptTypes.LET, ReScriptTypes.TYPE,
            ReScriptTypes.MODULE, ReScriptTypes.PRIVATE, ReScriptTypes.SWITCH,
            ReScriptTypes.IF, ReScriptTypes.ELSE,
            ReScriptTypes.LIDENT, ReScriptTypes.UIDENT,
        )
        // The prevIsExprEnd bit is ignorable here: it self-corrects on the first
        // significant token lexed after a restart (see PREV_IS_EXPR_END_MASK).
        checkZeroState(
            ReScriptLexerAdapter(),
            "type t = private { x: int }\nlet x = if foo { 1 } else { 2 }",
            tokens,
            _ReScriptLexer.PREV_IS_EXPR_END_MASK,
        )
    }

    @Test
    fun testCorrectRestart() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = if foo { 1 } else { 2 }")
    }

    @Test
    fun testCorrectRestartWithDivisionChain() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let a = x / y / z")
    }

    @Test
    fun testCorrectRestartAfterJsxSelfClosing() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let a = <A /> / b / c")
    }

    @Test
    fun testCorrectRestartAfterJsxClosedElement() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let a = <A></A> / b / c")
    }

    @Test
    fun testCorrectRestartWithComparison() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let ok = a < b && c > d || e <= f")
    }

    @Test
    fun testCorrectRestartWithJsx() {
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            """let x = <A icon={<B c="d" />}> <div>ok</div> {a < b ? y : z} txt </A>""",
        )
    }

    @Test
    fun testCorrectRestartWithJsxTemplateInterplay() {
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            """let a = <A t=`x ${DOLLAR}{n} y` /> ++ `s ${DOLLAR}{<div> `c ${DOLLAR}{m}` </div>} e`""",
        )
    }

    @Test
    fun testCorrectRestartWithTemplateInterpolation() {
        checkCorrectRestart(ReScriptLexerAdapter(), """let x = `hello ${DOLLAR}{name}`""")
    }

    @Test
    fun testCorrectRestartWithNestedTemplateInterpolation() {
        checkCorrectRestart(ReScriptLexerAdapter(), """let x = `outer ${DOLLAR}{`inner ${DOLLAR}{value}`}`""")
    }

    // ----- Saturation guards -----
    // Every packed counter saturates at its field maximum. The invariant these
    // tests pin is NOT that overflowing input lexes nicely (it may mis-scope
    // locally) — it is that a full lex and a restarted lex agree on the same
    // answer even past the limits, which is what incremental lexing needs.

    @Test
    fun testCorrectRestartWithCommentDepthOverflow() {
        // Comment depth field holds 0..3; this nests 4 deep.
        checkCorrectRestart(ReScriptLexerAdapter(), "/* a /* b /* c /* d */ */ */ */ let z = 1")
    }

    @Test
    fun testCorrectRestartWithTemplateBraceDepthOverflow() {
        // A TEMPLATE frame's brace depth holds 0..15; the interpolation plus
        // 16 nested braces ask for 17.
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            """let y = `v ${DOLLAR}{ {{{{{{{{{{{{{{{{ x }}}}}}}}}}}}}}}} }`""",
        )
    }

    @Test
    fun testCorrectRestartWithJsxAttrBraceDepthOverflow() {
        // A JSX_ATTR frame's brace depth holds 0..63; the attribute brace plus
        // 64 nested braces ask for 65.
        val opens = "{".repeat(64)
        val closes = "}".repeat(64)
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <a p={ $opens y $closes } />")
    }

    @Test
    fun testCorrectRestartWithJsxChildBraceDepthOverflow() {
        // A JSX_CONTENT frame's brace depth holds 0..7; the child brace plus
        // 8 nested braces ask for 9.
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <a> { {{{{{{{{ y }}}}}}}} } </a>")
    }

    @Test
    fun testCorrectRestartWithJsxChildCountOverflow() {
        // A JSX_CONTENT frame's child count holds 0..7; this nests 9 unbraced
        // elements in one children region.
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            "let w = <a><b><c><d><e><f><g><h><i>ok</i></h></g></f></e></d></c></b></a>",
        )
    }

    @Test
    fun testCorrectRestartWithContextStackOverflow() {
        // The context stack holds 3 frames; this element>brace alternation
        // asks for 5, so pushes drop the outermost frames.
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            "let x = <a> {y1 ++ <b> {y2 ++ <c> {y3 ++ <d> {y4 ++ <e> {y5} </e>} </d>} </c>} </b>} </a>",
        )
    }

    @Test
    fun testCorrectRestartWithJsxTagNewlineRescue() {
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            "let x = <div\nlet y = 1\n\nlet a = <div\n  let b = 2\n\nlet c = <input\n  type=\"text\"\n/>",
        )
    }

    @Test
    fun testCorrectRestartWithJsxTagNewlineRescueFramePop() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <outer><div\nlet y = {1}\nlet z = /ok/")
    }

    @Test
    fun testCorrectRestartWithJsxTagNewlineRescueInterpolation() {
        checkCorrectRestart(
            ReScriptLexerAdapter(),
            """let t = `before ${DOLLAR}{<outer>{<div
let y = 1
}</outer>} after`""",
        )
    }
}
