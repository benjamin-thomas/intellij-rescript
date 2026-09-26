package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import com.github.benjamin_thomas.intellij_rescript.ReScriptLanguage

class ReScriptStatementMover : LineMover() {

    private val movableTypes = TokenSet.create(
        ReScriptTypes.DECORATED_DECLARATION,
        ReScriptTypes.LET_BINDING,
        ReScriptTypes.MODULE_BINDING,
        ReScriptTypes.TYPE_DECLARATION,
        ReScriptTypes.OPEN_STATEMENT,
        ReScriptTypes.INCLUDE_STATEMENT,
        ReScriptTypes.EXTERNAL_DECLARATION,
        ReScriptTypes.EXCEPTION_DECLARATION,
        ReScriptTypes.EXTENSION_POINT,
        ReScriptTypes.TOP_LEVEL_EXPR,
    )

    private val jsxParentTags = TokenSet.create(
        ReScriptTypes.JSX_GT,
        ReScriptTypes.JSX_CLOSING_TAG,
    )

    override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {
        // Global extension point, so we must verify the language first
        if (file.language !is ReScriptLanguage) return false

        // Sets info.toMove below!
        if (!super.checkAvailable(editor, file, info, down)) return false

        val originalRange = info.toMove ?: return false
        val psiRange = getElementRange(editor, file, originalRange) ?: return false
        if (psiRange.first == null || psiRange.second == null) return false

        val firstChild = findJsxChild(psiRange.first)
        if (firstChild != null) {
            val lastChild = findJsxChild(psiRange.second) ?: firstChild
            val sibling = firstNonWhiteElement(
                if (down) lastChild.nextSibling else firstChild.prevSibling,
                down
            )
            if (sibling == null || sibling.node.elementType in jsxParentTags) {
                info.toMove2 = null
                return true
            }
            info.toMove = LineRange(firstChild, lastChild)
            info.toMove2 = LineRange(sibling)
            return true
        }

        val firstItem = findMovableAncestor(psiRange.first) ?: return false
        val lastItem = findMovableAncestor(psiRange.second) ?: return false

        val sibling = firstNonWhiteElement(
            if (down) lastItem.nextSibling else firstItem.prevSibling,
            down
        ) ?: run {
            info.toMove2 = null
            return true
        }

        if (sibling.node.elementType !in movableTypes) {
            info.toMove2 = null
            return true
        }

        info.toMove = LineRange(firstItem, lastItem)
        info.toMove2 = LineRange(sibling)
        return true
    }

    private fun findJsxChild(psi: PsiElement): PsiElement? {
        var current: PsiElement? = psi
        while (current != null && current !is PsiFile) {
            if (isInJsxChildren(current)) return current
            current = current.parent
        }
        return null
    }

    private fun isInJsxChildren(psi: PsiElement): Boolean {
        val parent = psi.parent ?: return false
        if (parent.node.elementType != ReScriptTypes.JSX_ELEMENT) return false
        val openingTagEnd = parent.node.findChildByType(ReScriptTypes.JSX_GT) ?: return false
        return psi.textRange.startOffset >= openingTagEnd.textRange.endOffset &&
            psi.node.elementType != ReScriptTypes.JSX_CLOSING_TAG
    }

    private fun findMovableAncestor(psi: PsiElement): PsiElement? {
        var current: PsiElement? = psi
        while (current != null) {
            if (current.node.elementType in movableTypes) {
                val isWrappedInDecorator = current.parent?.node?.elementType == ReScriptTypes.DECORATED_DECLARATION
                return if (isWrappedInDecorator) current.parent else current
            }
            current = current.parent
        }
        return null
    }
}
