package com.github.benjamin_thomas.intellij_rescript.lang

import kotlin.test.Test

class ReScriptLexerTest {
    private fun runLexerTest(inputFile: String, expectedOutputFile: String) =
        runSnapshotTest(ReScriptLexerAdapter(), inputFile, expectedOutputFile)

    @Test
    fun testKeywords() = runLexerTest("Keywords.res", "Keywords.txt")

    @Test
    fun testIdentifiers() = runLexerTest("Identifiers.res", "Identifiers.txt")
}
