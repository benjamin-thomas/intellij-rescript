package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.testFramework.ParsingTestCase

class ReScriptParserTest : ParsingTestCase(
    "com/github/benjamin_thomas/intellij_rescript/parser/fixtures",
    "res",
    false,
    ReScriptParserDefinition()
) {
    override fun getTestDataPath() = "src/test/resources"

    private fun runParserTest(inputFile: String, expectedOutputFile: String) =
        createParserTest(
            createAndSetPsiFile = { file ->
                val name = file.removeSuffix(".res")
                createPsiFile(name, loadFile(file)).also { myFile = it }
            },
            ensureNoErrorElements = ::ensureNoErrorElements,
            toParseTreeText = { toParseTreeText(it, false, false) },
            testDataPath = testDataPath,
            fullDataPath = myFullDataPath,
        )(inputFile, expectedOutputFile)

    fun testLetBinding() = runParserTest("LetBinding.res", "LetBinding.out")
}
