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

    // Dummy parser: consumes all tokens into a flat tree.
    // Will be replaced by a GrammarKit-generated parser in Phase 2.
    override fun createParser(project: Project): PsiParser =
        PsiParser { root, builder ->
            val marker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            marker.done(root)
            builder.treeBuilt
        }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet =
        TokenSet.create(ReScriptTokenTypes.LINE_COMMENT, ReScriptTokenTypes.BLOCK_COMMENT)

    override fun getStringLiteralElements(): TokenSet =
        TokenSet.create(ReScriptTokenTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement =
        throw UnsupportedOperationException("Not yet implemented")

    override fun createFile(viewProvider: FileViewProvider): PsiFile =
        ReScriptFile(viewProvider)
}
