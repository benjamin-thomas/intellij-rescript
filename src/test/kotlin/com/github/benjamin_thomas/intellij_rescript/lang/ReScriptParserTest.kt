package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.ParsingTestCase

private val noop: () -> Unit = {}

class ReScriptParserTest : ParsingTestCase(
    "com/github/benjamin_thomas/intellij_rescript/parser/fixtures",
    "res",
    false,
    ReScriptParserDefinition()
) {
    override fun getTestDataPath() = System.getProperty("user.dir") + "/src/test/resources"

    private fun runParserTest(inputFile: String, expectedOutputFile: String, hasParseErrors: Boolean = false) =
        createParserTest(
            createAndSetPsiFile = { file ->
                val name = file.removeSuffix(".res")
                createPsiFile(name, loadFile(file)).also { myFile = it }
            },
            ensureNoErrorElements = if (hasParseErrors) noop else ::ensureNoErrorElements,
            toParseTreeText = { toParseTreeText(it, false, false) },
            fullDataPath = myFullDataPath,
        )(inputFile, expectedOutputFile)

    fun testLetBinding() = runParserTest("LetBinding.res", "LetBinding.out")
    fun testMultipleDeclarations() = runParserTest("MultipleDeclarations.res", "MultipleDeclarations.out")
    fun testErrorRecovery() = runParserTest("ErrorRecovery.res", "ErrorRecovery.out", hasParseErrors = true)
}
