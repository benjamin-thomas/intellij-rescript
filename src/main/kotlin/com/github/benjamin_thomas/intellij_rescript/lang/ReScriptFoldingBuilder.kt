package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Native PSI-based code folding for ReScript.
 *
 * Folds braced blocks (`{ ... }`) that span multiple lines.
 * This is the first native feature — the ReScript LSP server does not
 * support foldingRangeProvider, so this cannot be done via LSP.
 */
class ReScriptFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        PsiTreeUtil.processElements(root) { element ->
            if (element.node.elementType == ReScriptTypes.LBRACE) {
                val rbrace = findMatchingRbrace(element)
                if (rbrace != null) {
                    val startLine = document.getLineNumber(element.textRange.startOffset)
                    val endLine = document.getLineNumber(rbrace.textRange.endOffset)
                    // Only fold if it spans multiple lines
                    if (endLine > startLine) {
                        val range = TextRange(element.textRange.startOffset, rbrace.textRange.endOffset)
                        descriptors.add(FoldingDescriptor(element.node, range))
                    }
                }
            }
            true
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "{...}"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    /**
     * Find the matching RBRACE for an LBRACE within the same parent node.
     * In our PSI tree, LBRACE and RBRACE are direct children of the declaration
     * node (e.g., LetBinding), so we scan forward through siblings.
     */
    private fun findMatchingRbrace(lbrace: PsiElement): PsiElement? {
        var current = lbrace.nextSibling
        var depth = 1
        while (current != null) {
            when (current.node.elementType) {
                ReScriptTypes.LBRACE -> depth++
                ReScriptTypes.RBRACE -> {
                    depth--
                    if (depth == 0) return current
                }
            }
            current = current.nextSibling
        }
        return null
    }
}
