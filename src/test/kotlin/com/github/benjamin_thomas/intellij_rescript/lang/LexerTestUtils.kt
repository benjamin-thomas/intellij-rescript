package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase
import java.io.File

private const val FIXTURES_DIR = "src/test/resources/com/github/benjamin_thomas/intellij_rescript/lexer/fixtures"

fun runSnapshotTest(lexer: Lexer, inputFile: String, expectedOutputFile: String) {
    val source = File(FIXTURES_DIR, inputFile)
    val gold = File(FIXTURES_DIR, expectedOutputFile)

    val fileText = FileUtil.loadFile(source, Charsets.UTF_8)
    val text = StringUtil.convertLineSeparators(fileText.trim())
    val result = LexerTestCase.printTokens(text, 0, lexer)
    UsefulTestCase.assertSameLinesWithFile(gold.canonicalPath, result)
}
