package com.github.benjamin_thomas.intellij_rescript.lang

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
}
