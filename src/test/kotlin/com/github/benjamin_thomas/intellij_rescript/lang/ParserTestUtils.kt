package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.PsiFile
import com.intellij.testFramework.UsefulTestCase.assertSameLinesWithFile
import java.io.File

// ParsingTestCase exposes its internals as protected methods, forcing subclass inheritance.
// We work around this by capturing those methods as lambdas from inside the subclass,
// so the actual test logic can live here as a standalone function.
fun createParserTest(
    createAndSetPsiFile: (String) -> PsiFile,
    ensureNoErrorElements: () -> Unit,
    toParseTreeText: (PsiFile) -> String,
    testDataPath: String,
    fullDataPath: String,
): (String, String) -> Unit = { inputFile, expectedOutputFile ->
    val psiFile = createAndSetPsiFile(inputFile)
    ensureNoErrorElements()
    val actual = toParseTreeText(psiFile)
    val goldPath = File(testDataPath, "$fullDataPath/$expectedOutputFile").canonicalPath
    assertSameLinesWithFile(goldPath, actual)
}
