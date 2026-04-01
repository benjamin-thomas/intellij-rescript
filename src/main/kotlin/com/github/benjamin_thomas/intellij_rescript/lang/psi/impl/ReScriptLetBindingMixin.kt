package com.github.benjamin_thomas.intellij_rescript.lang.psi.impl

import com.github.benjamin_thomas.intellij_rescript.lang.ReScriptTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.TokenType
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptBindingPattern

// See _knowledge/parser/PSI_CUSTOMIZATION.md for how mixins and PsiNameIdentifierOwner work.
abstract class ReScriptLetBindingMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement? {
        val pattern = findChildByClass(ReScriptBindingPattern::class.java) ?: return null
        var child = pattern.firstChild
        while (child != null) {
            val type = child.node.elementType
            when (type) {
                TokenType.WHITE_SPACE,
                ReScriptTypes.LINE_COMMENT,
                ReScriptTypes.BLOCK_COMMENT -> { /* skip trivia */ }
                ReScriptTypes.LIDENT -> return child
                else -> return null
            }
            child = child.nextSibling
        }
        return null
    }

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        TODO("Rename not yet implemented for LetBinding")
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
