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

    @JvmField val WHITE_SPACE = TokenType.WHITE_SPACE
    @JvmField val BAD_CHARACTER = TokenType.BAD_CHARACTER
}
