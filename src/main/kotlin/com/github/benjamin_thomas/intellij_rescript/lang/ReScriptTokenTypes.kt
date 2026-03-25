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

    @JvmField val WHITE_SPACE = TokenType.WHITE_SPACE
    @JvmField val BAD_CHARACTER = TokenType.BAD_CHARACTER
}
