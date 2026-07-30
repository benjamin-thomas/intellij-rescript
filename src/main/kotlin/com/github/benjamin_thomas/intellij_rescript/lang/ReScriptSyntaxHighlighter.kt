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
        val STRING_ESCAPE = createTextAttributesKey("RESCRIPT_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
        val LINE_COMMENT = createTextAttributesKey("RESCRIPT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BLOCK_COMMENT = createTextAttributesKey("RESCRIPT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val OPERATOR = createTextAttributesKey("RESCRIPT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val DELIMITER = createTextAttributesKey("RESCRIPT_DELIMITER", DefaultLanguageHighlighterColors.BRACKETS)
        val COMMA = createTextAttributesKey("RESCRIPT_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val SEMICOLON = createTextAttributesKey("RESCRIPT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val DOT = createTextAttributesKey("RESCRIPT_DOT", DefaultLanguageHighlighterColors.DOT)
        val DECORATOR = createTextAttributesKey("RESCRIPT_DECORATOR", DefaultLanguageHighlighterColors.METADATA)
        val REGEX = createTextAttributesKey("RESCRIPT_REGEX", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
        // Fallback to KEYWORD: every bundled theme styles it, unlike MARKUP_TAG
        // which renders as plain text in the common dark themes. (The XML
        // plugin's HTML_TAG_NAME would be semantically nicer but isn't on this
        // plugin's classpath.) Users can still restyle via the key itself.
        val JSX_PUNCTUATION = createTextAttributesKey("RESCRIPT_JSX_PUNCTUATION", DefaultLanguageHighlighterColors.KEYWORD)
        // Same trade-off as JSX_PUNCTUATION: MARKUP_TAG / MARKUP_ATTRIBUTE render
        // as plain text in the common dark themes, so components borrow the class
        // color, intrinsic tags the keyword color, attribute names the field color.
        val JSX_COMPONENT_NAME =
            createTextAttributesKey("RESCRIPT_JSX_COMPONENT_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
        val JSX_TAG_NAME =
            createTextAttributesKey("RESCRIPT_JSX_TAG_NAME", DefaultLanguageHighlighterColors.KEYWORD)
        val JSX_ATTRIBUTE_NAME =
            createTextAttributesKey("RESCRIPT_JSX_ATTRIBUTE_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    }

    override fun getHighlightingLexer(): Lexer = ReScriptLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when (tokenType) {
            ReScriptTypes.LET, ReScriptTypes.TYPE, ReScriptTypes.MODULE,
            ReScriptTypes.OPEN, ReScriptTypes.INCLUDE, ReScriptTypes.EXTERNAL,
            ReScriptTypes.EXCEPTION, ReScriptTypes.REC, ReScriptTypes.TRUE, ReScriptTypes.FALSE,
            ReScriptTypes.SWITCH, ReScriptTypes.IF, ReScriptTypes.ELSE,
            ReScriptTypes.ASYNC, ReScriptTypes.AWAIT, ReScriptTypes.TRY, ReScriptTypes.CATCH,
            ReScriptTypes.WHILE, ReScriptTypes.FOR, ReScriptTypes.AND, ReScriptTypes.AS -> KEYWORD

            ReScriptTypes.LIDENT -> IDENTIFIER
            ReScriptTypes.UIDENT -> TYPE_NAME

            ReScriptTypes.BIGINT, ReScriptTypes.INT, ReScriptTypes.FLOAT -> NUMBER
            ReScriptTypes.STRING_START, ReScriptTypes.STRING_END,
            ReScriptTypes.STRING_CONTENT,
            ReScriptTypes.TEMPLATE_START, ReScriptTypes.TEMPLATE_END,
            ReScriptTypes.TEMPLATE_CONTENT -> STRING
            ReScriptTypes.STRING_ESCAPE -> STRING_ESCAPE
            ReScriptTypes.REGEX -> REGEX

            ReScriptTypes.LINE_COMMENT -> LINE_COMMENT
            ReScriptTypes.BLOCK_COMMENT -> BLOCK_COMMENT

            ReScriptTypes.EQ, ReScriptTypes.PLUS, ReScriptTypes.MINUS,
            ReScriptTypes.STAR, ReScriptTypes.SLASH,
            ReScriptTypes.EQEQ, ReScriptTypes.EQEQEQ,
            ReScriptTypes.BANGEQ, ReScriptTypes.BANGEQEQ,
            ReScriptTypes.AMPAMPAMP, ReScriptTypes.AMPAMP,
            ReScriptTypes.PIPEPIPEPIPE, ReScriptTypes.PIPEPIPE,
            ReScriptTypes.CARETCARETCARET,
            ReScriptTypes.TILDETILDETILDE,
            ReScriptTypes.SHIFT_LEFT, ReScriptTypes.SHIFT_RIGHT, ReScriptTypes.SHIFT_RIGHT_UNSIGNED,
            ReScriptTypes.STARSTAR,
            ReScriptTypes.LT, ReScriptTypes.GT,
            ReScriptTypes.LTEQ, ReScriptTypes.GTEQ,
            ReScriptTypes.ARROW, ReScriptTypes.FAT_ARROW,
            ReScriptTypes.PIPE_FORWARD, ReScriptTypes.PIPE,
            ReScriptTypes.PLUS_DOT, ReScriptTypes.MINUS_DOT,
            ReScriptTypes.STAR_DOT, ReScriptTypes.SLASH_DOT,
            ReScriptTypes.COLONGT, ReScriptTypes.DOTDOT,
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

            ReScriptTypes.JSX_LT, ReScriptTypes.JSX_LT_SLASH,
            ReScriptTypes.JSX_SLASH_GT, ReScriptTypes.JSX_GT -> JSX_PUNCTUATION

            ReScriptTypes.AT -> DECORATOR
            ReScriptTypes.UNDERSCORE -> KEYWORD

            else -> return emptyArray()
        }
        return arrayOf(key)
    }
}
