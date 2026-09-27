package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.psi.tree.TokenSet
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

private val RESOURCES_DIR = File(System.getProperty("user.dir"), "src/test/resources")
private val FIXTURES_DIR = File(RESOURCES_DIR, "com/github/benjamin_thomas/intellij_rescript/lexer/fixtures").path
private val ACTUAL_DIR = File(System.getProperty("user.dir"), "build/gold-actual")

/**
 * A mismatch's expected and actual texts travel inside the
 * FileComparisonFailedError, which only the IDE's test runner unpacks: Gradle's
 * console and XML get the message alone. So the actual text also goes under
 * build/gold-actual, at the gold's path, and the message says so.
 */
fun assertSameLinesWithGold(goldPath: String, actual: String) {
    try {
        assertSameLinesWithFile(goldPath, actual)
    } catch (e: FileComparisonFailedError) {
        val relative = File(goldPath).canonicalFile.relativeToOrSelf(RESOURCES_DIR.canonicalFile).path
        val copy = File(ACTUAL_DIR, relative)
        copy.parentFile.mkdirs()
        copy.writeText(actual)
        throw FileComparisonFailedError(
            "${e.message}\nActual text written to ${copy.path}: diff it against the gold.",
            e.expectedStringPresentation, e.actualStringPresentation, goldPath, copy.path,
        )
    }
}

fun runSnapshotTest(lexer: Lexer, inputFile: String, expectedOutputFile: String) {
    val source = File(FIXTURES_DIR, inputFile)
    val gold = File(FIXTURES_DIR, expectedOutputFile)

    val fileText = FileUtil.loadFile(source, Charsets.UTF_8)
    val text = StringUtil.convertLineSeparators(fileText.trim())
    val result = LexerTestCase.printTokens(text, 0, lexer)
    assertSameLinesWithGold(gold.canonicalPath, result)
}

/** A lexer fixture's text as runSnapshotTest lexes it, for checks a snapshot cannot make. */
fun fixtureText(inputFile: String): String =
    StringUtil.convertLineSeparators(FileUtil.loadFile(File(FIXTURES_DIR, inputFile), Charsets.UTF_8).trim())

/** Token dump for assertions a snapshot cannot make — `runSnapshotTest` normalizes line endings. */
fun lexTokens(lexer: Lexer, text: String): String = LexerTestCase.printTokens(text, 0, lexer)

/**
 * Verifies that certain token types always leave the lexer in state zero.
 * Important for incremental re-lexing: IntelliJ re-lexes from mid-file when
 * the user edits, and tokens in non-zero state can cause incorrect re-lexing.
 */
fun checkZeroState(lexer: Lexer, text: String, tokenTypes: TokenSet, ignorableStateBits: Int = 0) {
    lexer.start(text)
    while (true) {
        val type = lexer.tokenType ?: break
        if (tokenTypes.contains(type) && (lexer.state and ignorableStateBits.inv()) != 0) {
            fail(
                "Non-zero lexer state on token \"${lexer.tokenText}\" ($type) at ${lexer.tokenStart}"
            )
        }
        lexer.advance()
    }
}

/**
 * Verifies the lexer produces identical tokens when restarted from every
 * token boundary. This tests incremental lexing correctness: when a user
 * edits a file, IntelliJ restarts the lexer from a saved position rather
 * than from the beginning.
 */
fun checkCorrectRestart(lexer: Lexer, text: String) {
    // First pass: collect all tokens
    data class TokenInfo(
        val type: String, val start: Int, val end: Int, val text: String,
        val state: Int, val exact: Boolean,
    )

    val tokens = mutableListOf<TokenInfo>()
    lexer.start(text)
    while (lexer.tokenType != null) {
        tokens.add(
            TokenInfo(
                type = lexer.tokenType.toString(),
                start = lexer.tokenStart,
                end = lexer.tokenEnd,
                text = lexer.tokenText,
                state = lexer.state,
                exact = (lexer as? ReScriptLexerAdapter)?.isStateExact() ?: true,
            )
        )
        lexer.advance()
    }

    // Second pass: restart from each token's position and verify the remaining tokens match
    for (i in tokens.indices) {
        val token = tokens[i]

        if (!token.exact) {
            // The restart int cannot describe a context this deep, so restarting
            // here would not reproduce the stream. That is only safe because the
            // platform never picks such a boundary — it restarts where the state
            // equals the lexer's initial state. Assert exactly that, so the skip
            // can never hide a state that looks restartable.
            assertNotEquals(
                0, token.state,
                "Inexact restart state at ${token.start} (token $i) reported the initial state, " +
                    "so the platform could restart into it",
            )
            continue
        }

        lexer.start(text, token.start, text.length, token.state)

        for (j in i until tokens.size) {
            val expected = tokens[j]
            val actualType = lexer.tokenType?.toString()
            assertEquals(
                expected.type, actualType,
                "Token mismatch after restart at position ${token.start} (token $i), checking token $j"
            )
            assertEquals(
                expected.text, lexer.tokenText,
                "Token text mismatch after restart at position ${token.start}"
            )
            lexer.advance()
        }
    }
}

/** Offsets whose restart state the packed int cannot describe exactly. */
fun inexactRestartOffsets(text: String): List<Int> {
    val lexer = ReScriptLexerAdapter()
    val offsets = mutableListOf<Int>()
    lexer.start(text)
    while (lexer.tokenType != null) {
        if (!lexer.isStateExact()) offsets.add(lexer.tokenStart)
        lexer.advance()
    }
    return offsets
}
