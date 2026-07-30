package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptJsxAttribute
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptJsxTagName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

private val JSX_NAME_TOKENS = TokenSet.create(ReScriptTypes.LIDENT, ReScriptTypes.UIDENT)

class ReScriptJsxAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is ReScriptJsxTagName -> annotateTagName(element, holder)
            is ReScriptJsxAttribute -> element.node.findChildByType(ReScriptTypes.LIDENT)?.psi?.let {
                annotate(it, ReScriptSyntaxHighlighter.JSX_ATTRIBUTE_NAME, holder)
            }
        }
    }

    private fun annotateTagName(element: ReScriptJsxTagName, holder: AnnotationHolder) {
        val key = if (element.firstChild.node.elementType == ReScriptTypes.UIDENT) {
            ReScriptSyntaxHighlighter.JSX_COMPONENT_NAME
        } else {
            ReScriptSyntaxHighlighter.JSX_TAG_NAME
        }

        element.node.getChildren(JSX_NAME_TOKENS)
            .forEach { annotate(it.psi, key, holder) }
    }

    private fun annotate(element: PsiElement, key: TextAttributesKey, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(key)
            .create()
    }
}
