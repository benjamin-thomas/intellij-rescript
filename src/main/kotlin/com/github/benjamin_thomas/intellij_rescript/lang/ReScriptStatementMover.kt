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
        ReScriptTypes.LET_BINDING,
        ReScriptTypes.MODULE_BINDING,
        ReScriptTypes.TYPE_DECLARATION,
        ReScriptTypes.OPEN_STATEMENT,
        ReScriptTypes.INCLUDE_STATEMENT,
        ReScriptTypes.EXTERNAL_DECLARATION,
        ReScriptTypes.EXCEPTION_DECLARATION,
        ReScriptTypes.EXTENSION_POINT,
    )

    override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {
        // Global extension point, so we must verify the language first
        if (file.language !is ReScriptLanguage) return false

        // Sets info.toMove below!
        if (!super.checkAvailable(editor, file, info, down)) return false

        val originalRange = info.toMove ?: return false
        val psiRange = getElementRange(editor, file, originalRange) ?: return false
        if (psiRange.first == null || psiRange.second == null) return false

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

    private fun findMovableAncestor(psi: PsiElement): PsiElement? {
        var current: PsiElement? = psi
        while (current != null) {
            if (current.node.elementType in movableTypes) return current
            current = current.parent
        }
        return null
    }
}
