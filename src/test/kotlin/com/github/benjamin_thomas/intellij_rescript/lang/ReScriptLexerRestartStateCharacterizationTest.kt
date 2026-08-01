package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import java.io.File
import kotlin.test.Test

class ReScriptLexerRestartStateCharacterizationTest {
    @Test
    fun testFixturePackedRestartStates() {
        val projectRoot = File(System.getProperty("user.dir"))
        val fixturesRoot = File(
            projectRoot,
            "src/test/resources/com/github/benjamin_thomas/intellij_rescript",
        )
        val gold = File(
            projectRoot,
            "src/test/resources/com/github/benjamin_thomas/intellij_rescript/lexer/fixtures/RestartStates.out",
        )
        val result = buildString {
            fixturesRoot.walkTopDown()
                .filter { it.isFile && it.extension == "res" }
                .sortedBy { it.relativeTo(projectRoot).invariantSeparatorsPath }
                .forEach { file ->
                    val fixture = file.relativeTo(projectRoot).invariantSeparatorsPath
                    val text = StringUtil.convertLineSeparators(FileUtil.loadFile(file, Charsets.UTF_8))
                    val lexer = ReScriptLexerAdapter()
                    lexer.start(text)
                    while (lexer.tokenType != null) {
                        append(fixture)
                        append(' ')
                        append(lexer.tokenStart)
                        append(' ')
                        append(lexer.tokenType)
                        append(' ')
                        append("0x")
                        append(lexer.state.toUInt().toString(16).padStart(8, '0'))
                        append(' ')
                        append(lexer.isStateExact())
                        append('\n')
                        lexer.advance()
                    }
                }
        }
        assertSameLinesWithFile(gold.canonicalPath, result)
    }
}
