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
    fun testStringEmpty() = runLexerTest("StringEmpty.res", "StringEmpty.out")

    @Test
    fun testStringUnclosed() = runLexerTest("StringUnclosed.res", "StringUnclosed.out")

    @Test
    fun testTemplateSimple() = runLexerTest("TemplateSimple.res", "TemplateSimple.out")

    @Test
    fun testTemplateWithSpaces() = runLexerTest("TemplateWithSpaces.res", "TemplateWithSpaces.out")

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
    fun testTemplateEmpty() = runLexerTest("TemplateEmpty.res", "TemplateEmpty.out")

    @Test
    fun testTemplateUnclosed() = runLexerTest("TemplateUnclosed.res", "TemplateUnclosed.out")

    @Test
    fun testZeroStateForKeywordsAndIdentifiers() {
        val tokens = TokenSet.create(
            ReScriptTypes.LET, ReScriptTypes.TYPE,
            ReScriptTypes.MODULE, ReScriptTypes.SWITCH,
            ReScriptTypes.IF, ReScriptTypes.ELSE,
            ReScriptTypes.LIDENT, ReScriptTypes.UIDENT,
        )
        checkZeroState(ReScriptLexerAdapter(), "let x = if foo { 1 } else { 2 }", tokens)
    }

    @Test
    fun testCorrectRestart() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = if foo { 1 } else { 2 }")
    }

    @Test
    fun testCorrectRestartWithTemplateInterpolation() {
        checkCorrectRestart(ReScriptLexerAdapter(), """let x = `hello ${DOLLAR}{name}`""")
    }
}
