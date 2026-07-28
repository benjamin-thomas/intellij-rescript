package com.github.benjamin_thomas.intellij_rescript.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class ReScriptSyntaxHighlighterTest {

    @Test
    fun testJsxPunctuationMapsToJsxKey() {
        val highlighter = ReScriptSyntaxHighlighter()
        for (token in listOf(
            ReScriptTypes.JSX_LT,
            ReScriptTypes.JSX_LT_SLASH,
            ReScriptTypes.JSX_SLASH_GT,
            ReScriptTypes.JSX_GT,
        )) {
            val keys = highlighter.getTokenHighlights(token)
            assertEquals(1, keys.size, "number of highlight keys for $token")
            assertEquals(
                "RESCRIPT_JSX_PUNCTUATION",
                keys.single().externalName,
                "highlight key for $token",
            )
        }
    }
}
