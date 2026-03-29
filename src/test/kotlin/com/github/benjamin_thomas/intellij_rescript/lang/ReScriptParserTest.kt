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

    /**
     * @param hasParseErrors skip the "no PsiErrorElement" assertion (for intentionally broken input)
     * @param skipSpaces hide PsiWhiteSpace nodes from the gold file (default: true for readability)
     * @param printRanges show character offset ranges on each node, e.g. `PsiElement(LET)('let')[0,3]`
     */
    private fun runParserTest(
        inputFile: String,
        expectedOutputFile: String,
        hasParseErrors: Boolean = false,
        skipSpaces: Boolean = true,
        printRanges: Boolean = false,
    ) =
        createParserTest(
            createAndSetPsiFile = { file ->
                val name = file.removeSuffix(".res")
                createPsiFile(name, loadFile(file)).also { myFile = it }
            },
            ensureNoErrorElements = if (hasParseErrors) noop else ::ensureNoErrorElements,
            toParseTreeText = { toParseTreeText(it, skipSpaces, printRanges) },
            fullDataPath = myFullDataPath,
        )(inputFile, expectedOutputFile)

    fun testLetBinding() = runParserTest("LetBinding.res", "LetBinding.out")
    fun testModuleBinding() = runParserTest("ModuleBinding.res", "ModuleBinding.out")
    fun testModuleAlias() = runParserTest("ModuleAlias.res", "ModuleAlias.out")
    fun testMultipleDeclarations() = runParserTest("MultipleDeclarations.res", "MultipleDeclarations.out")
    fun testArrowFunction() = runParserTest("ArrowFunction.res", "ArrowFunction.out")
    fun testTypeWithParams() = runParserTest("TypeWithParams.res", "TypeWithParams.out")
    fun testNestedLetBindings() = runParserTest("NestedLetBindings.res", "NestedLetBindings.out")
    fun testNestedDelimiters() = runParserTest("NestedDelimiters.res", "NestedDelimiters.out")
    fun testExtensionPoint() = runParserTest("ExtensionPoint.res", "ExtensionPoint.out")
    fun testDecorator() = runParserTest("Decorator.res", "Decorator.out")
    fun testDecoratorAfterLet() = runParserTest("DecoratorAfterLet.res", "DecoratorAfterLet.out")
    fun testAllDeclarationTypes() = runParserTest("AllDeclarationTypes.res", "AllDeclarationTypes.out")
    fun testEmptyFile() = runParserTest("EmptyFile.res", "EmptyFile.out")
    fun testStackedDecorators() = runParserTest("StackedDecorators.res", "StackedDecorators.out")
fun testCommentsBetweenDeclarations() = runParserTest("CommentsBetweenDeclarations.res", "CommentsBetweenDeclarations.out")
    fun testExpressionExtensionPoint() = runParserTest("ExpressionExtensionPoint.res", "ExpressionExtensionPoint.out")
    fun testErrorRecovery() = runParserTest("ErrorRecovery.res", "ErrorRecovery.out", hasParseErrors = true)
    fun testErrorRecoveryInBlock() = runParserTest("ErrorRecoveryInBlock.res", "ErrorRecoveryInBlock.out", hasParseErrors = true)
}
