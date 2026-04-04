package com.github.benjamin_thomas.intellij_rescript.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

class ReScriptTemplateLiteralManipulator : AbstractElementManipulator<ReScriptTemplateLiteral>() {

    override fun getRangeInElement(element: ReScriptTemplateLiteral): TextRange {
        // Exclude the opening and closing backticks
        return TextRange(1, element.textLength - 1)
    }

    override fun handleContentChange(
        element: ReScriptTemplateLiteral,
        range: TextRange,
        newContent: String,
    ): ReScriptTemplateLiteral {
        // TODO: implement when rename/refactoring needs it
        return element
    }
}
