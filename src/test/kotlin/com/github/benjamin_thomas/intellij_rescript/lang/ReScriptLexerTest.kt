package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.TokenSet
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

    // Includes four levels: a depth counter saturating at three loses the fourth
    // increment, closes one `*/` early, and leaks the last one out as STAR SLASH.
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

    // Two closing tags on one line: without children context the `/` after `<`
    // enters the REGEX state and swallows `/div></s` as one regex literal.
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

    // Regression: a completed self-closing element is an
    // expression end, so a following `/` is division (not a regex start) and
    // a following `<` is comparison (not another tag).
    @Test
    fun testJsxAfterSelfClosing() = runLexerTest("JsxAfterSelfClosing.res", "JsxAfterSelfClosing.out")

    // Same, for elements completed by a closing tag or fragment close.
    @Test
    fun testJsxAfterClosedElement() = runLexerTest("JsxAfterClosedElement.res", "JsxAfterClosedElement.out")

    // ...unless a line break intervenes: a `<` that opens a line is a tag even
    // after an expression end, which is how an element in statement position
    // gets lexed. Same-line `a<b` and `a < b` stay comparisons.
    @Test
    fun testJsxStatementPosition() = runLexerTest("JsxStatementPosition.res", "JsxStatementPosition.out")

    // A lone CR is not a line break to ReScript — `a` CR `<b` is a comparison.
    // Cannot be a snapshot: the fixture loader normalizes line endings away.
    @Test
    fun testBareCarriageReturnIsNotALineBreak() {
        val tokens = lexTokens(ReScriptLexerAdapter(), "let ok = a\r<b")
        assertTrue(tokens.contains("LT ('<')"), tokens)
        assertFalse(tokens.contains("JSX_LT"), tokens)
    }

    // ...but the LF of a CRLF pair still is.
    @Test
    fun testCarriageReturnLineFeedIsALineBreak() {
        val tokens = lexTokens(ReScriptLexerAdapter(), "let x = 1\r\n<div />")
        assertTrue(tokens.contains("JSX_LT ('<')"), tokens)
    }

    // --- The packed restart-state contract ------------------------------
    // Part 1 (exact boundaries reproduce the stream) and part 2 (an inexact
    // boundary is never the initial state, so the platform cannot restart into
    // it) live inside checkCorrectRestart. Parts 3 and 4 are here.

    // Part 3: the skip in part 1 must not quietly grow to cover ordinary code.
    // Every fixture in the suite has to round-trip, save the ones that exist to
    // exceed the int.
    @Test
    fun testFixtureRestartStatesAreExact() {
        val deliberatelyDeep = setOf("JsxNestedRegionOverflow.res")
        val root = File(
            System.getProperty("user.dir") +
                "/src/test/resources/com/github/benjamin_thomas/intellij_rescript"
        )
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "res" && it.name !in deliberatelyDeep }
            .mapNotNull { file ->
                val text = StringUtil.convertLineSeparators(FileUtil.loadFile(file, Charsets.UTF_8))
                inexactRestartOffsets(text).firstOrNull()?.let { "${file.name}@$it" }
            }
            .toList()
        assertTrue(
            offenders.isEmpty(),
            "Fixtures whose restart state no longer round-trips: $offenders. Either the fixture " +
                "got deeper than the packed int, or the packing regressed — do not just add it " +
                "to deliberatelyDeep without deciding which.",
        )
    }

    // Part 4: pin WHERE inexactness begins. Without this the hint could rot to
    // "never exact" and parts 1-3 would still pass.
    @Test
    fun testRestartStateExactnessBoundary() {
        val three = "let x = <ul> {<li className={`p ${DOLLAR}{q}`} />} </ul>"
        val four = "let x = <ul> {<li> <span className={`p ${DOLLAR}{q}`} /> </li>} </ul>"
        assertTrue(
            inexactRestartOffsets(three).isEmpty(),
            "three live regions must still round-trip through the packed int",
        )
        assertTrue(
            inexactRestartOffsets(four).isNotEmpty(),
            "four live regions cannot fit the packed int — if they now do, the hint changed",
        )
    }

    // The counter clamp is the other way a state goes inexact, and it rots
    // independently of the frame count, so it gets its own pin: the packed
    // child count holds seven.
    @Test
    fun testRestartStateExactnessCounterBoundary() {
        fun nest(depth: Int): String {
            val names = ('a'..'z').take(depth)
            val open = names.joinToString("") { "<$it>" }
            val close = names.reversed().joinToString("") { "</$it>" }
            return "let x = $open {\"z\"->React.string} $close"
        }
        assertTrue(
            inexactRestartOffsets(nest(7)).isEmpty(),
            "seven nested unbraced elements must still round-trip",
        )
        assertTrue(
            inexactRestartOffsets(nest(8)).isNotEmpty(),
            "eight cannot — the packed child count holds seven",
        )
    }

    @Test
    fun testDelimiters() = runLexerTest("Delimiters.res", "Delimiters.out")

    @Test
    fun testDecorators() = runLexerTest("Decorators.res", "Decorators.out")

    @Test
    fun testSpecial() = runLexerTest("Special.res", "Special.out")

    @Test
    fun testRegex() = runLexerTest("Regex.res", "Regex.out")

    // A completed regex literal is a value, so it ends an expression like any
    // other literal: the `<` after it compares and the `/` after it divides.
    // bsc agrees — `/a/<b` is `Pexp_apply "<"` over the literal.
    @Test
    fun testRegexIsExpressionEnd() = runLexerTest("RegexIsExpressionEnd.res", "RegexIsExpressionEnd.out")

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
        // Both decision bits are ignorable here: this check is about lexical
        // regions, and neither bit puts the lexer inside one. The first token on
        // a line always carries SAW_LINE_BREAK — that is the point of the bit,
        // not a region (see its declaration).
        checkZeroState(
            ReScriptLexerAdapter(),
            "type t = private { x: int }\nlet x = if foo { 1 } else { 2 }",
            tokens,
            _ReScriptLexer.PREV_IS_EXPR_END_MASK or _ReScriptLexer.SAW_LINE_BREAK_MASK,
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

    // A completed regex is an expression end, so the token after one is where
    // the expression-end predicate and the line-break bit meet. Nothing else restarts
    // across a finished regex — every other regex input ends the string.
    @Test
    fun testCorrectRestartAfterRegexLiteral() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let ok = /a/<b\nlet jsx = /a/\n<div />")
    }

    // `(`/`[` in a tag open a JSX_VALUE region whose interior lexes as ordinary
    // expression text. Nothing else restarts across one, so these cover a
    // restart landing on the opener, inside the arguments, and on the closer.
    @Test
    fun testCorrectRestartWithJsxValueRegion() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let v = <A b=Some(<d />) c=[1, 2] />")
    }

    @Test
    fun testCorrectRestartWithJsxValueTemplate() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let v = <A b=f(`x ${DOLLAR}{y}`) />")
    }

    @Test
    fun testCorrectRestartWithJsxValueNestedBraces() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let v = <A b=f({x: 1}, [g(2)]).h />")
    }

    @Test
    fun testCorrectRestartWithUnclosedJsxValue() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let v = <A b=f(\nlet w = 1")
    }

    // Comments in JSX positions. The block-comment state records where it was
    // entered from in a live field, so these pin that a restart either side of
    // one still agrees with a full lex.
    @Test
    fun testCorrectRestartWithCommentInTag() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <A /* c */ b=1 // d\n/>")
    }

    @Test
    fun testCorrectRestartWithCommentInChildrenAndCloseTag() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <A b=1>/* c */ hi </A /* d */>")
    }

    @Test
    fun testCorrectRestartWithNestedCommentInTag() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <A /* a /* b */ c\n*/ b=1 />")
    }

    @Test
    fun testCorrectRestartWithCommentInsideAttributeBraces() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = <A b={<B /* c */ d=1 />}>e</A>")
    }

    // The whole reason the block-comment return state can be a plain field: no
    // token boundary ever sits inside a comment. If someone later makes the
    // comment state emit tokens (doc-comment sub-tokens, say), this fires and
    // says the return state has to move into the packed restart int.
    @Test
    fun testBlockCommentStateIsNeverObservable() {
        val text = "let a = /* x /* y */ z */ 1\n" +
            "let b = <A /* c */ d=1>/* e */ f </A /* g */>\n"
        val lexer = ReScriptLexerAdapter()
        lexer.start(text)
        while (lexer.tokenType != null) {
            assertNotEquals(
                _ReScriptLexer.IN_BLOCK_COMMENT shr 1,
                lexer.state and _ReScriptLexer.LEXICAL_STATE_MASK,
                "a token boundary at ${lexer.tokenStart} is inside a block comment",
            )
            lexer.advance()
        }
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

    // A statement-position `<` opens a tag on the strength of the preceding
    // line break, which rides in bit 4 of the packed restart state. These pin
    // that a restart landing exactly on the `<` — the token whose decision
    // depends on what came before it — still agrees with a full lex.
    @Test
    fun testCorrectRestartWithJsxInStatementPosition() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let f = () => {\n  let x = 1\n\n  <div />\n}")
    }

    @Test
    fun testCorrectRestartWithJsxStatementSiblings() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let f = () => {\n  g()\n\n  <div>\n    <A />\n    <B />\n  </div>\n}")
    }

    @Test
    fun testCorrectRestartWithSameLineComparison() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let ok = a<b\nlet ko = c <d")
    }

    // The break may hide inside a block comment — bsc counts it, so these must
    // restart identically or an edit inside the comment could strand the `<`.
    @Test
    fun testCorrectRestartWithCommentOpeningTheLine() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = 1\n/* c */ <div />")
    }

    @Test
    fun testCorrectRestartWithLineBreakInsideBlockComment() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = 1 /* c\n*/ <div />")
    }

    @Test
    fun testCorrectRestartWithLineBreakInsideNestedBlockComment() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = 1 /* a /* b */ c\n*/ <div />")
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
    // The comment depth saturates at its packed width, and there the invariant
    // is that a full lex and a restarted lex agree on the same (wrong) answer.
    //
    // The frame stack and its counters work the other way: they are live
    // at full width and only their packed copy is lossy, so past three frames
    // (or seven nested children) these inputs lex CORRECTLY and most of their
    // boundaries are inexact — checkCorrectRestart skips those, having first
    // asserted they can never be selected as restart points. What these tests
    // still pin is that nothing crashes or diverges at the boundaries that do
    // round-trip; the depth of the exact/inexact frontier is pinned separately,
    // by the two exactness-boundary tests above.

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
