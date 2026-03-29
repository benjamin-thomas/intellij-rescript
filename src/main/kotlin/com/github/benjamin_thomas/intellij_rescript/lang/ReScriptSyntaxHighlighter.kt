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
        val REGEX = createTextAttributesKey("RESCRIPT_REGEX", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    }

    override fun getHighlightingLexer(): Lexer = ReScriptLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when (tokenType) {
            ReScriptTypes.LET, ReScriptTypes.TYPE, ReScriptTypes.MODULE,
            ReScriptTypes.OPEN, ReScriptTypes.INCLUDE, ReScriptTypes.EXTERNAL,
            ReScriptTypes.EXCEPTION, ReScriptTypes.REC, ReScriptTypes.TRUE, ReScriptTypes.FALSE,
            ReScriptTypes.SWITCH, ReScriptTypes.IF, ReScriptTypes.ELSE -> KEYWORD

            ReScriptTypes.LIDENT -> IDENTIFIER
            ReScriptTypes.UIDENT -> TYPE_NAME

            ReScriptTypes.INT, ReScriptTypes.FLOAT -> NUMBER
            ReScriptTypes.STRING -> STRING
            ReScriptTypes.REGEX -> REGEX

            ReScriptTypes.LINE_COMMENT -> LINE_COMMENT
            ReScriptTypes.BLOCK_COMMENT -> BLOCK_COMMENT

            ReScriptTypes.EQ, ReScriptTypes.PLUS, ReScriptTypes.MINUS,
            ReScriptTypes.STAR, ReScriptTypes.SLASH,
            ReScriptTypes.EQEQ, ReScriptTypes.BANGEQ, ReScriptTypes.AMPAMP, ReScriptTypes.PIPEPIPE,
            ReScriptTypes.LT, ReScriptTypes.GT,
            ReScriptTypes.LTEQ, ReScriptTypes.GTEQ,
            ReScriptTypes.ARROW, ReScriptTypes.FAT_ARROW,
            ReScriptTypes.PIPE_FORWARD, ReScriptTypes.PIPE,
            ReScriptTypes.PLUS_DOT, ReScriptTypes.MINUS_DOT,
            ReScriptTypes.STAR_DOT, ReScriptTypes.SLASH_DOT,
            ReScriptTypes.BANG, ReScriptTypes.QUESTION,
            ReScriptTypes.HASH, ReScriptTypes.TILDE,
            ReScriptTypes.DOTDOTDOT -> OPERATOR

            ReScriptTypes.LPAREN, ReScriptTypes.RPAREN,
            ReScriptTypes.LBRACE, ReScriptTypes.RBRACE,
            ReScriptTypes.LBRACKET, ReScriptTypes.RBRACKET -> DELIMITER

            ReScriptTypes.COMMA -> COMMA
            ReScriptTypes.SEMICOLON -> SEMICOLON
            ReScriptTypes.DOT -> DOT
            ReScriptTypes.COLON -> OPERATOR

            ReScriptTypes.AT -> DECORATOR
            ReScriptTypes.UNDERSCORE -> KEYWORD

            else -> return emptyArray()
        }
        return arrayOf(key)
    }
}
