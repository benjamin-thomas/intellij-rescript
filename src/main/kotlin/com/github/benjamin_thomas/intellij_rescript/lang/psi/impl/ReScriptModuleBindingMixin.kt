package com.github.benjamin_thomas.intellij_rescript.lang.psi.impl

import com.github.benjamin_thomas.intellij_rescript.lang.ReScriptTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

abstract class ReScriptModuleBindingMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement? =
        findChildByType(ReScriptTypes.UIDENT)

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        TODO("Rename not yet implemented for ModuleBinding")
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
