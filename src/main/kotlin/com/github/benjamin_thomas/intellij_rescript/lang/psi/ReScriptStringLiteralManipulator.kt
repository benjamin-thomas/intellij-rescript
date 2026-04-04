package com.github.benjamin_thomas.intellij_rescript.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

class ReScriptStringLiteralManipulator : AbstractElementManipulator<ReScriptStringLiteral>() {

    override fun getRangeInElement(element: ReScriptStringLiteral): TextRange {
        // Exclude the opening and closing quotes
        return TextRange(1, element.textLength - 1)
    }

    override fun handleContentChange(
        element: ReScriptStringLiteral,
        range: TextRange,
        newContent: String,
    ): ReScriptStringLiteral {
        // TODO: implement when rename/refactoring needs it
        return element
    }
}
