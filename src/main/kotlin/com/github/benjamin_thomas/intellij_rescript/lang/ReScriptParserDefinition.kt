package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.github.benjamin_thomas.intellij_rescript.ReScriptFile
import com.github.benjamin_thomas.intellij_rescript.ReScriptLanguage

private val FILE = IFileElementType(ReScriptLanguage)

class ReScriptParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = ReScriptLexerAdapter()

    override fun createParser(project: Project): PsiParser = ReScriptParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet =
        TokenSet.create(ReScriptTypes.LINE_COMMENT, ReScriptTypes.BLOCK_COMMENT)

    override fun getStringLiteralElements(): TokenSet = ReScriptTokenSets.STRING_LITERALS

    override fun createElement(node: ASTNode): PsiElement =
        ReScriptTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        ReScriptFile(viewProvider)
}
