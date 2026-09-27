package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
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

    private val jsxParents = TokenSet.create(
        ReScriptTypes.JSX_ELEMENT,
        ReScriptTypes.JSX_FRAGMENT,
    )

    private val jsxClosingTags = TokenSet.create(
        ReScriptTypes.JSX_CLOSING_TAG,
        ReScriptTypes.JSX_LT_SLASH,
    )

    private val openingBrackets = TokenSet.create(
        ReScriptTypes.LBRACE,
        ReScriptTypes.LPAREN,
        ReScriptTypes.LBRACKET,
    )

    private val closingBrackets = TokenSet.create(
        ReScriptTypes.RBRACE,
        ReScriptTypes.RPAREN,
        ReScriptTypes.RBRACKET,
    )

    override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {
        // Global extension point, so we must verify the language first
        if (file.language !is ReScriptLanguage) return false

        // Sets info.toMove below!
        if (!super.checkAvailable(editor, file, info, down)) return false

        val originalRange = info.toMove ?: return false
        val psiRange = getElementRange(editor, file, originalRange) ?: return false
        if (psiRange.first == null || psiRange.second == null) return false

        val jsxChild = findJsxChild(psiRange.first)
        if (jsxChild != null) {
            val groups = jsxChildLineGroups(jsxChild.parent, editor.document)
            val firstIndex = groups.indexOfFirst { it.last >= originalRange.startLine }
            val lastIndex = groups.indexOfLast { it.first < originalRange.endLine }
            val targetIndex = if (down) lastIndex + 1 else firstIndex - 1
            if (firstIndex == -1 || lastIndex < firstIndex || targetIndex !in groups.indices) {
                info.toMove2 = null
                return true
            }
            info.toMove = LineRange(groups[firstIndex].first, groups[lastIndex].last + 1)
            info.toMove2 = LineRange(groups[targetIndex].first, groups[targetIndex].last + 1)
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
            // A declaration inside a braced child is also a loose JSX child: it wins.
            if (current.node.elementType in movableTypes) return null
            if (isInJsxChildren(current)) return current
            current = current.parent
        }
        return null
    }

    // Braced children are loose leaves (bracedBlock is a private rule), so a
    // child is rebuilt by bracket depth; children sharing a line move together.
    // A group on a line of the parent's tags stays put: its line holds the tag.
    private fun jsxChildLineGroups(parent: PsiElement, document: Document): List<IntRange> {
        val children = jsxChildrenRange(parent) ?: return emptyList()
        val groups = mutableListOf<IntRange>()
        var depth = 0
        for (child in generateSequence(parent.firstChild) { it.nextSibling }) {
            if (child is PsiWhiteSpace || !children.contains(child.textRange)) continue
            val lines = document.getLineNumber(child.textRange.startOffset)..
                document.getLineNumber(child.textRange.endOffset)
            val last = groups.lastOrNull()
            if (last != null && (depth > 0 || lines.first <= last.last)) {
                groups[groups.lastIndex] = last.first..maxOf(last.last, lines.last)
            } else {
                groups += lines
            }
            depth = when (child.node.elementType) {
                in openingBrackets -> depth + 1
                in closingBrackets -> maxOf(0, depth - 1)
                else -> depth
            }
        }
        val openingTagLine = document.getLineNumber(children.startOffset)
        val closingTagLine = document.getLineNumber(children.endOffset)
        return groups.filter { it.first > openingTagLine && it.last < closingTagLine }
    }

    private fun isInJsxChildren(psi: PsiElement): Boolean {
        val children = psi.parent?.let(::jsxChildrenRange) ?: return false
        return children.contains(psi.textRange)
    }

    private fun jsxChildrenRange(parent: PsiElement): TextRange? {
        if (parent.node.elementType !in jsxParents) return null
        val openingTagEnd = parent.node.findChildByType(ReScriptTypes.JSX_GT) ?: return null
        val closingTag = parent.node.findChildByType(jsxClosingTags) ?: return null
        return TextRange(openingTagEnd.textRange.endOffset, closingTag.startOffset)
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
