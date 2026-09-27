package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

object ReScriptParserUtil : GeneratedParserUtilBase() {
    /**
     * `list` or `dict` glued to a `{`: bsc scans the pair as one token, which is
     * the only thing telling `b=list{1}` (one value) from `b=list {...p}` (a
     * value, then a spread attribute). The glue is a raw-token fact the grammar
     * cannot see, since PsiBuilder skips whitespace.
     */
    @JvmStatic
    fun gluedCollectionBrace(builder: PsiBuilder, level: Int): Boolean =
        builder.tokenType == ReScriptTypes.LIDENT &&
            (builder.tokenText == "list" || builder.tokenText == "dict") &&
            builder.rawLookup(1) == ReScriptTypes.LBRACE
}
