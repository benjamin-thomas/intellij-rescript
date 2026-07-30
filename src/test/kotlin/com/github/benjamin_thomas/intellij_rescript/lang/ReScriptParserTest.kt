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
    fun testLetFirstClassModuleExpression() =
        runParserTest("LetFirstClassModuleExpression.res", "LetFirstClassModuleExpression.out")
    fun testLetFirstClassModuleExpressionWithConstraint() =
        runParserTest(
            "LetFirstClassModuleExpressionWithConstraint.res",
            "LetFirstClassModuleExpressionWithConstraint.out"
        )
    fun testLetFirstClassModuleParameter() =
        runParserTest("LetFirstClassModuleParameter.res", "LetFirstClassModuleParameter.out")
    fun testLetFirstClassModuleUnpack() =
        runParserTest("LetFirstClassModuleUnpack.res", "LetFirstClassModuleUnpack.out")
    fun testModuleBinding() = runParserTest("ModuleBinding.res", "ModuleBinding.out")
    fun testModuleAlias() = runParserTest("ModuleAlias.res", "ModuleAlias.out")
    fun testMultipleDeclarations() = runParserTest("MultipleDeclarations.res", "MultipleDeclarations.out")
    fun testArrowFunction() = runParserTest("ArrowFunction.res", "ArrowFunction.out")
    fun testTypeWithParams() = runParserTest("TypeWithParams.res", "TypeWithParams.out")
    fun testTypeFirstClassModule() = runParserTest("TypeFirstClassModule.res", "TypeFirstClassModule.out")
    fun testTypeFirstClassModuleWithTypeEquations() =
        runParserTest("TypeFirstClassModuleWithTypeEquations.res", "TypeFirstClassModuleWithTypeEquations.out")
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
    fun testStandaloneAttribute() = runParserTest("StandaloneAttribute.res", "StandaloneAttribute.out")
    fun testStandaloneAttributeInModule() =
        runParserTest("StandaloneAttributeInModule.res", "StandaloneAttributeInModule.out")
    fun testStandaloneAttributeInModuleType() =
        runParserTest("StandaloneAttributeInModuleType.res", "StandaloneAttributeInModuleType.out")

    fun testCommentsBetweenDeclarations() =
        runParserTest("CommentsBetweenDeclarations.res", "CommentsBetweenDeclarations.out")

    fun testExpressionExtensionPoint() = runParserTest("ExpressionExtensionPoint.res", "ExpressionExtensionPoint.out")
    fun testErrorRecovery() = assertParserDoesNotCrash("ErrorRecovery.res")
    fun testErrorRecoveryInBlock() = assertParserDoesNotCrash("ErrorRecoveryInBlock.res")
    fun testLetRec() = runParserTest("LetRec.res", "LetRec.out")
    fun testTypeAnd() = runParserTest("TypeAnd.res", "TypeAnd.out")
    fun testTypeAndChain() = runParserTest("TypeAndChain.res", "TypeAndChain.out")
    fun testLetRecAnd() = runParserTest("LetRecAnd.res", "LetRecAnd.out")
    fun testDecoratedAndType() = runParserTest("DecoratedAndType.res", "DecoratedAndType.out")
    fun testDecoratedAndLet() = runParserTest("DecoratedAndLet.res", "DecoratedAndLet.out")
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
    fun testTypeBodyDoesNotConsumeFollowingDecorator() =
        runParserTest("TypeBodyDoesNotConsumeFollowingDecorator.res", "TypeBodyDoesNotConsumeFollowingDecorator.out")
    fun testPrivateTypeBodyDoesNotConsumeFollowingDecorator() =
        runParserTest(
            "PrivateTypeBodyDoesNotConsumeFollowingDecorator.res",
            "PrivateTypeBodyDoesNotConsumeFollowingDecorator.out"
        )
    fun testRecordFieldAttribute() = runParserTest("RecordFieldAttribute.res", "RecordFieldAttribute.out")
    fun testVariantConstructorAttribute() =
        runParserTest("VariantConstructorAttribute.res", "VariantConstructorAttribute.out")
    fun testLetSignature() = runParserTest("LetSignature.res", "LetSignature.out")
    fun testModuleWithSignature() = runParserTest("ModuleWithSignature.res", "ModuleWithSignature.out")
    fun testModuleTypeDeclaration() = runParserTest("ModuleTypeDeclaration.res", "ModuleTypeDeclaration.out")
    fun testModuleTypeAlias() = runParserTest("ModuleTypeAlias.res", "ModuleTypeAlias.out")
    fun testStringLiteral() = runParserTest("StringLiteral.res", "StringLiteral.out")
    fun testStringLiteralWithEscape() = runParserTest("StringLiteralWithEscape.res", "StringLiteralWithEscape.out")
    fun testStringLiteralEmpty() = runParserTest("StringLiteralEmpty.res", "StringLiteralEmpty.out")
    fun testTemplateLiteral() = runParserTest("TemplateLiteral.res", "TemplateLiteral.out")
    fun testTemplateLiteralWithEscapedBackticks() =
        runParserTest("TemplateLiteralWithEscapedBackticks.res", "TemplateLiteralWithEscapedBackticks.out")
    fun testTemplateLiteralInterpolation() =
        runParserTest("TemplateLiteralInterpolation.res", "TemplateLiteralInterpolation.out")
    fun testTemplateLiteralNestedInterpolation() =
        runParserTest("TemplateLiteralNestedInterpolation.res", "TemplateLiteralNestedInterpolation.out")
    fun testTemplateLiteralEmpty() = runParserTest("TemplateLiteralEmpty.res", "TemplateLiteralEmpty.out")
    fun testTopLevelSwitch() = runParserTest("TopLevelSwitch.res", "TopLevelSwitch.out")
    fun testTopLevelExprThenLet() = runParserTest("TopLevelExprThenLet.res", "TopLevelExprThenLet.out")

    // A JSX element used as a JSX attribute value, whose inner element carries a
    // quoted-literal attribute, must parse without error. The trailing `/` of the
    // inner `<B ... />` used to be lexed as a regex start (because STRING_END /
    // TEMPLATE_END were not expression-end tokens), swallowing the `/>` and the
    // enclosing `}` and leaving the JSX expression unbalanced.
    fun testJsxElementAsAttributeValue() =
        runParserTest("JsxElementAsAttributeValue.res", "JsxElementAsAttributeValue.out")
    fun testJsxElementAsAttributeValueTemplateAttr() =
        runParserTest("JsxElementAsAttributeValueTemplateAttr.res", "JsxElementAsAttributeValueTemplateAttr.out")
    fun testJsxElementAsAttributeValueNoInnerAttr() =
        runParserTest("JsxElementAsAttributeValueNoInnerAttr.res", "JsxElementAsAttributeValueNoInnerAttr.out")
    // Two levels of JSX-as-attribute nesting. The middle `/>` follows a `}` (RBRACE),
    // and the trailing `/>` provides a later `/` — so the middle slash would start a
    // regex that swallows the `}` closing the outer attribute, unbalancing the braces.
    // Guards that RBRACE is also treated as an expression-end token.
    fun testJsxNestedElementAsAttributeValue() =
        runParserTest("JsxNestedElementAsAttributeValue.res", "JsxNestedElementAsAttributeValue.out")
    fun testJsxChildrenElements() =
        runParserTest("JsxChildrenElements.res", "JsxChildrenElements.out")
    fun testJsxSelfClosingElement() =
        runParserTest("JsxSelfClosingElement.res", "JsxSelfClosingElement.out")
    fun testJsxStringAttribute() =
        runParserTest("JsxStringAttribute.res", "JsxStringAttribute.out")
    fun testJsxComponentTagName() =
        runParserTest("JsxComponentTagName.res", "JsxComponentTagName.out")
    fun testJsxDottedTagName() =
        runParserTest("JsxDottedTagName.res", "JsxDottedTagName.out")
    fun testJsxModuleValueTagName() =
        runParserTest("JsxModuleValueTagName.res", "JsxModuleValueTagName.out")
    fun testJsxDeepModuleValueTagName() =
        runParserTest("JsxDeepModuleValueTagName.res", "JsxDeepModuleValueTagName.out")
    // Malformed JSX must produce a localized error inside JSX_ELEMENT, not soup, not a cascade.
    fun testJsxInvalidLowerDottedTagName() =
        runParserTest("JsxInvalidLowerDottedTagName.res", "JsxInvalidLowerDottedTagName.out", hasParseErrors = true)
    fun testJsxInvalidMidLowercaseTagName() =
        runParserTest("JsxInvalidMidLowercaseTagName.res", "JsxInvalidMidLowercaseTagName.out", hasParseErrors = true)
    fun testJsxAttributePunned() =
        runParserTest("JsxAttributePunned.res", "JsxAttributePunned.out")
    fun testJsxAttributeValueForms() =
        runParserTest("JsxAttributeValueForms.res", "JsxAttributeValueForms.out")
    fun testJsxSpreadAttribute() =
        runParserTest("JsxSpreadAttribute.res", "JsxSpreadAttribute.out")
    // A braced attribute without `...` is a syntax error, so it must not be
    // structured as a spread.
    fun testJsxInvalidBareBracedAttribute() =
        runParserTest("JsxInvalidBareBracedAttribute.res", "JsxInvalidBareBracedAttribute.out", hasParseErrors = true)
    fun testJsxEmptyChildren() =
        runParserTest("JsxEmptyChildren.res", "JsxEmptyChildren.out")
    fun testJsxElementChild() =
        runParserTest("JsxElementChild.res", "JsxElementChild.out")
    fun testJsxBracedChild() =
        runParserTest("JsxBracedChild.res", "JsxBracedChild.out")
    fun testJsxTextChild() =
        runParserTest("JsxTextChild.res", "JsxTextChild.out")
    fun testJsxNestedElements() =
        runParserTest("JsxNestedElements.res", "JsxNestedElements.out")
    // BNF cannot correlate tag names: a mismatch parses clean (the compiler rejects it).
    fun testJsxMismatchedClosingTag() =
        runParserTest("JsxMismatchedClosingTag.res", "JsxMismatchedClosingTag.out")
    fun testJsxVariantChild() =
        runParserTest("JsxVariantChild.res", "JsxVariantChild.out")
    fun testJsxExtensionChild() =
        runParserTest("JsxExtensionChild.res", "JsxExtensionChild.out")
    fun testJsxFirstClassModuleChild() =
        runParserTest("JsxFirstClassModuleChild.res", "JsxFirstClassModuleChild.out")
    fun testJsxInvalidStandaloneDotChild() =
        runParserTest("JsxInvalidStandaloneDotChild.res", "JsxInvalidStandaloneDotChild.out", hasParseErrors = true)
    fun testJsxPathChildren() =
        runParserTest("JsxPathChildren.res", "JsxPathChildren.out")
    fun testJsxInvalidPathChildren() =
        runParserTest("JsxInvalidPathChildren.res", "JsxInvalidPathChildren.out", hasParseErrors = true)
    fun testJsxEmptyFragment() =
        runParserTest("JsxEmptyFragment.res", "JsxEmptyFragment.out")
    fun testJsxFragmentElementChild() =
        runParserTest("JsxFragmentElementChild.res", "JsxFragmentElementChild.out")
    fun testJsxFragmentBracedChild() =
        runParserTest("JsxFragmentBracedChild.res", "JsxFragmentBracedChild.out")
    fun testJsxNestedFragment() =
        runParserTest("JsxNestedFragment.res", "JsxNestedFragment.out")
    fun testJsxBracedElementChild() =
        runParserTest("JsxBracedElementChild.res", "JsxBracedElementChild.out")
    fun testJsxMissingClosingTag() =
        runParserTest("JsxMissingClosingTag.res", "JsxMissingClosingTag.out", hasParseErrors = true)
    fun testJsxUnterminatedClosingTag() =
        runParserTest("JsxUnterminatedClosingTag.res", "JsxUnterminatedClosingTag.out", hasParseErrors = true)
    // An unclosed brace captures what follows — the same behaviour as any unclosed block,
    // and the lexer reads `</div>` there as comparison soup, not closing-tag tokens.
    fun testJsxUnterminatedBracedChild() =
        runParserTest("JsxUnterminatedBracedChild.res", "JsxUnterminatedBracedChild.out", hasParseErrors = true)
    fun testJsxStrayClosingDelimiter() =
        runParserTest("JsxStrayClosingDelimiter.res", "JsxStrayClosingDelimiter.out", hasParseErrors = true)
    fun testJsxExcludedFromOpaqueBody() =
        runParserTest("JsxExcludedFromOpaqueBody.res", "JsxExcludedFromOpaqueBody.out")
    fun testJsxExcludedFromTypeBody() =
        runParserTest("JsxExcludedFromTypeBody.res", "JsxExcludedFromTypeBody.out")
    fun testJsxLiteralAttributeValues() =
        runParserTest("JsxLiteralAttributeValues.res", "JsxLiteralAttributeValues.out")
    // `true` stays LIDENT here: the JSX_TAG lexer state has no keyword rules.
    fun testJsxIdentAttributeValues() =
        runParserTest("JsxIdentAttributeValues.res", "JsxIdentAttributeValues.out")
    fun testJsxHyphenatedTagNames() =
        runParserTest("JsxHyphenatedTagNames.res", "JsxHyphenatedTagNames.out")
    fun testJsxHyphenatedOpenClose() =
        runParserTest("JsxHyphenatedOpenClose.res", "JsxHyphenatedOpenClose.out")
    fun testJsxHyphenatedAttributeNames() =
        runParserTest("JsxHyphenatedAttributeNames.res", "JsxHyphenatedAttributeNames.out")
    fun testJsxInvalidHyphenPathStart() =
        runParserTest("JsxInvalidHyphenPathStart.res", "JsxInvalidHyphenPathStart.out", hasParseErrors = true)
    // `neg=-1` is a syntax error (bsc): the attribute must stay punned and no
    // JSX_ATTRIBUTE_VALUE may form — JsxAttributeValue must never admit MINUS.
    fun testJsxInvalidNegativeAttributeValue() =
        runParserTest("JsxInvalidNegativeAttributeValue.res", "JsxInvalidNegativeAttributeValue.out", hasParseErrors = true)
    fun testJsxTagNewlineRescue() =
        runParserTest("JsxTagNewlineRescue.res", "JsxTagNewlineRescue.out", hasParseErrors = true)
    fun testJsxComponentsPlayground() =
        runParserTest("JsxComponents.res", "JsxComponents.out")
}
