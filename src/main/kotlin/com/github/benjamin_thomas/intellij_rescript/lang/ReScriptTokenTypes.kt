package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.TokenType

object ReScriptTokenTypes {
    @JvmField val LET = ReScriptTokenType("LET")
    @JvmField val TYPE = ReScriptTokenType("TYPE")
    @JvmField val MODULE = ReScriptTokenType("MODULE")
    @JvmField val SWITCH = ReScriptTokenType("SWITCH")
    @JvmField val IF = ReScriptTokenType("IF")
    @JvmField val ELSE = ReScriptTokenType("ELSE")

    @JvmField val LIDENT = ReScriptTokenType("LIDENT")
    @JvmField val UIDENT = ReScriptTokenType("UIDENT")

    @JvmField val INT = ReScriptTokenType("INT")
    @JvmField val FLOAT = ReScriptTokenType("FLOAT")
    @JvmField val STRING = ReScriptTokenType("STRING")

    @JvmField val LINE_COMMENT = ReScriptTokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = ReScriptTokenType("BLOCK_COMMENT")

    @JvmField val EQ = ReScriptTokenType("EQ")
    @JvmField val PLUS = ReScriptTokenType("PLUS")
    @JvmField val MINUS = ReScriptTokenType("MINUS")
    @JvmField val STAR = ReScriptTokenType("STAR")
    @JvmField val SLASH = ReScriptTokenType("SLASH")
    @JvmField val EQEQ = ReScriptTokenType("EQEQ")
    @JvmField val BANGEQ = ReScriptTokenType("BANGEQ")
    @JvmField val LT = ReScriptTokenType("LT")
    @JvmField val GT = ReScriptTokenType("GT")
    @JvmField val LTEQ = ReScriptTokenType("LTEQ")
    @JvmField val GTEQ = ReScriptTokenType("GTEQ")
    @JvmField val ARROW = ReScriptTokenType("ARROW")
    @JvmField val FAT_ARROW = ReScriptTokenType("FAT_ARROW")
    @JvmField val PIPE_FORWARD = ReScriptTokenType("PIPE_FORWARD")
    @JvmField val PLUS_DOT = ReScriptTokenType("PLUS_DOT")
    @JvmField val MINUS_DOT = ReScriptTokenType("MINUS_DOT")
    @JvmField val STAR_DOT = ReScriptTokenType("STAR_DOT")
    @JvmField val SLASH_DOT = ReScriptTokenType("SLASH_DOT")

    @JvmField val LPAREN = ReScriptTokenType("LPAREN")
    @JvmField val RPAREN = ReScriptTokenType("RPAREN")
    @JvmField val LBRACE = ReScriptTokenType("LBRACE")
    @JvmField val RBRACE = ReScriptTokenType("RBRACE")
    @JvmField val LBRACKET = ReScriptTokenType("LBRACKET")
    @JvmField val RBRACKET = ReScriptTokenType("RBRACKET")
    @JvmField val COMMA = ReScriptTokenType("COMMA")
    @JvmField val SEMICOLON = ReScriptTokenType("SEMICOLON")
    @JvmField val COLON = ReScriptTokenType("COLON")
    @JvmField val DOT = ReScriptTokenType("DOT")
    @JvmField val AT = ReScriptTokenType("AT")

    @JvmField val WHITE_SPACE = TokenType.WHITE_SPACE
    @JvmField val BAD_CHARACTER = TokenType.BAD_CHARACTER
}
