package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.tree.TokenSet
import kotlin.test.Test

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
}
