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
    fun testOperators() = runLexerTest("Operators.res", "Operators.out")

    @Test
    fun testDelimiters() = runLexerTest("Delimiters.res", "Delimiters.out")

    @Test
    fun testDecorators() = runLexerTest("Decorators.res", "Decorators.out")

    @Test
    fun testSpecial() = runLexerTest("Special.res", "Special.out")

    @Test
    fun testZeroStateForKeywordsAndIdentifiers() {
        val tokens = TokenSet.create(
            ReScriptTokenTypes.LET, ReScriptTokenTypes.TYPE,
            ReScriptTokenTypes.MODULE, ReScriptTokenTypes.SWITCH,
            ReScriptTokenTypes.IF, ReScriptTokenTypes.ELSE,
            ReScriptTokenTypes.LIDENT, ReScriptTokenTypes.UIDENT,
        )
        checkZeroState(ReScriptLexerAdapter(), "let x = if foo { 1 } else { 2 }", tokens)
    }

    @Test
    fun testCorrectRestart() {
        checkCorrectRestart(ReScriptLexerAdapter(), "let x = if foo { 1 } else { 2 }")
    }
}
