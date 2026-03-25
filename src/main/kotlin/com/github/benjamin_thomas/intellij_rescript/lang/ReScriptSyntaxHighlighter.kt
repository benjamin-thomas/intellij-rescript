package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class ReScriptSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        val KEYWORD = createTextAttributesKey("RESCRIPT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val IDENTIFIER = createTextAttributesKey("RESCRIPT_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val TYPE_NAME = createTextAttributesKey("RESCRIPT_TYPE_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
        val NUMBER = createTextAttributesKey("RESCRIPT_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val STRING = createTextAttributesKey("RESCRIPT_STRING", DefaultLanguageHighlighterColors.STRING)
        val LINE_COMMENT = createTextAttributesKey("RESCRIPT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BLOCK_COMMENT = createTextAttributesKey("RESCRIPT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val OPERATOR = createTextAttributesKey("RESCRIPT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val DELIMITER = createTextAttributesKey("RESCRIPT_DELIMITER", DefaultLanguageHighlighterColors.BRACKETS)
        val COMMA = createTextAttributesKey("RESCRIPT_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val SEMICOLON = createTextAttributesKey("RESCRIPT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val DOT = createTextAttributesKey("RESCRIPT_DOT", DefaultLanguageHighlighterColors.DOT)
        val DECORATOR = createTextAttributesKey("RESCRIPT_DECORATOR", DefaultLanguageHighlighterColors.METADATA)
    }

    override fun getHighlightingLexer(): Lexer = ReScriptLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when (tokenType) {
            ReScriptTokenTypes.LET, ReScriptTokenTypes.TYPE, ReScriptTokenTypes.MODULE,
            ReScriptTokenTypes.SWITCH, ReScriptTokenTypes.IF, ReScriptTokenTypes.ELSE -> KEYWORD

            ReScriptTokenTypes.LIDENT -> IDENTIFIER
            ReScriptTokenTypes.UIDENT -> TYPE_NAME

            ReScriptTokenTypes.INT, ReScriptTokenTypes.FLOAT -> NUMBER
            ReScriptTokenTypes.STRING -> STRING

            ReScriptTokenTypes.LINE_COMMENT -> LINE_COMMENT
            ReScriptTokenTypes.BLOCK_COMMENT -> BLOCK_COMMENT

            ReScriptTokenTypes.EQ, ReScriptTokenTypes.PLUS, ReScriptTokenTypes.MINUS,
            ReScriptTokenTypes.STAR, ReScriptTokenTypes.SLASH,
            ReScriptTokenTypes.EQEQ, ReScriptTokenTypes.BANGEQ,
            ReScriptTokenTypes.LT, ReScriptTokenTypes.GT,
            ReScriptTokenTypes.LTEQ, ReScriptTokenTypes.GTEQ,
            ReScriptTokenTypes.ARROW, ReScriptTokenTypes.FAT_ARROW,
            ReScriptTokenTypes.PIPE_FORWARD, ReScriptTokenTypes.PIPE,
            ReScriptTokenTypes.PLUS_DOT, ReScriptTokenTypes.MINUS_DOT,
            ReScriptTokenTypes.STAR_DOT, ReScriptTokenTypes.SLASH_DOT,
            ReScriptTokenTypes.BANG, ReScriptTokenTypes.QUESTION,
            ReScriptTokenTypes.HASH, ReScriptTokenTypes.TILDE,
            ReScriptTokenTypes.DOTDOTDOT -> OPERATOR

            ReScriptTokenTypes.LPAREN, ReScriptTokenTypes.RPAREN,
            ReScriptTokenTypes.LBRACE, ReScriptTokenTypes.RBRACE,
            ReScriptTokenTypes.LBRACKET, ReScriptTokenTypes.RBRACKET -> DELIMITER

            ReScriptTokenTypes.COMMA -> COMMA
            ReScriptTokenTypes.SEMICOLON -> SEMICOLON
            ReScriptTokenTypes.DOT -> DOT
            ReScriptTokenTypes.COLON -> OPERATOR

            ReScriptTokenTypes.AT -> DECORATOR
            ReScriptTokenTypes.UNDERSCORE -> KEYWORD

            else -> return emptyArray()
        }
        return arrayOf(key)
    }
}
