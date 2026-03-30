package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator

class ReScriptQuoteHandler : SimpleTokenSetQuoteHandler(
    ReScriptTypes.STRING_START,
    ReScriptTypes.STRING_END,
    ReScriptTypes.TEMPLATE_START,
    ReScriptTypes.TEMPLATE_END,
) {
    // The default implementation doesn't detect unclosed strings with our multi-token
    // design. Auto-pair only when the cursor is on a fresh opening quote (STRING_START
    // or TEMPLATE_START), not when inside an existing string.
    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.tokenType
        return tokenType == ReScriptTypes.STRING_START || tokenType == ReScriptTypes.TEMPLATE_START
    }
}
