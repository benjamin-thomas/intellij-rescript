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

    /** Verify that the parser doesn't crash on the given input (no gold file comparison). */
    private fun assertParserDoesNotCrash(inputFile: String) {
        val name = inputFile.removeSuffix(".res")
        createPsiFile(name, loadFile(inputFile))
    }

    fun testLetBinding() = runParserTest("LetBinding.res", "LetBinding.out")
    fun testModuleBinding() = runParserTest("ModuleBinding.res", "ModuleBinding.out")
    fun testModuleAlias() = runParserTest("ModuleAlias.res", "ModuleAlias.out")
    fun testMultipleDeclarations() = runParserTest("MultipleDeclarations.res", "MultipleDeclarations.out")
    fun testArrowFunction() = runParserTest("ArrowFunction.res", "ArrowFunction.out")
    fun testTypeWithParams() = runParserTest("TypeWithParams.res", "TypeWithParams.out")
    fun testNestedLetBindings() = runParserTest("NestedLetBindings.res", "NestedLetBindings.out")
    fun testNestedDelimiters() = runParserTest("NestedDelimiters.res", "NestedDelimiters.out")
    fun testExtensionPoint() = runParserTest("ExtensionPoint.res", "ExtensionPoint.out")
    fun testDecoratedDeclarationAfterLet() =
        runParserTest("DecoratedDeclarationAfterLet.res", "DecoratedDeclarationAfterLet.out")

    fun testOpenStatement() = runParserTest("OpenStatement.res", "OpenStatement.out")
    fun testIncludeStatement() = runParserTest("IncludeStatement.res", "IncludeStatement.out")
    fun testExternalDeclaration() = runParserTest("ExternalDeclaration.res", "ExternalDeclaration.out")
    fun testExceptionDeclaration() = runParserTest("ExceptionDeclaration.res", "ExceptionDeclaration.out")
    fun testEmptyFile() = runParserTest("EmptyFile.res", "EmptyFile.out")
    fun testStackedDecoratedDeclaration() =
        runParserTest("StackedDecoratedDeclaration.res", "StackedDecoratedDeclaration.out")

    fun testCommentsBetweenDeclarations() =
        runParserTest("CommentsBetweenDeclarations.res", "CommentsBetweenDeclarations.out")

    fun testExpressionExtensionPoint() = runParserTest("ExpressionExtensionPoint.res", "ExpressionExtensionPoint.out")
    fun testErrorRecovery() = assertParserDoesNotCrash("ErrorRecovery.res")
    fun testErrorRecoveryInBlock() = assertParserDoesNotCrash("ErrorRecoveryInBlock.res")
    fun testLetRec() = runParserTest("LetRec.res", "LetRec.out")
    fun testLetDestructuring() = runParserTest("LetDestructuring.res", "LetDestructuring.out")
    fun testLetDiscard() = runParserTest("LetDiscard.res", "LetDiscard.out")
    fun testDecoratedDeclaration() = runParserTest("DecoratedDeclaration.res", "DecoratedDeclaration.out")
    fun testDecoratedType() = runParserTest("DecoratedType.res", "DecoratedType.out")
    fun testDecoratedModule() = runParserTest("DecoratedModule.res", "DecoratedModule.out")
    fun testDecoratedExternal() = runParserTest("DecoratedExternal.res", "DecoratedExternal.out")
    fun testDecoratedOpen() = runParserTest("DecoratedOpen.res", "DecoratedOpen.out")
    fun testDecoratedInclude() = runParserTest("DecoratedInclude.res", "DecoratedInclude.out")
    fun testDecoratedExceptionDecl() = runParserTest("DecoratedExceptionDecl.res", "DecoratedExceptionDecl.out")
    fun testDecoratedExtensionPoint() = runParserTest("DecoratedExtensionPoint.res", "DecoratedExtensionPoint.out")
    fun testDecoratedExternalWithModule() =
        runParserTest("DecoratedExternalWithModule.res", "DecoratedExternalWithModule.out")
}
