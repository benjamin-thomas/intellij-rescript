package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.psi.tree.TokenSet

object ReScriptTokenSets {
    // All string/template tokens including escapes — used by ParserDefinition
    // to tell the platform what constitutes a string literal (spell checking,
    // "Find in String Literals", language injection).
    val STRING_LITERALS: TokenSet = TokenSet.create(
        ReScriptTypes.STRING_START, ReScriptTypes.STRING_END,
        ReScriptTypes.STRING_CONTENT, ReScriptTypes.STRING_ESCAPE,
        ReScriptTypes.TEMPLATE_START, ReScriptTypes.TEMPLATE_END,
        ReScriptTypes.TEMPLATE_CONTENT,
    )

}
