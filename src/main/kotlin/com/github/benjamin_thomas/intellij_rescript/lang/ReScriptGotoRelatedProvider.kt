package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.navigation.GotoRelatedItem
import com.intellij.navigation.GotoRelatedProvider
import com.intellij.psi.PsiElement

class ReScriptGotoRelatedProvider : GotoRelatedProvider() {
    override fun getItems(psiElement: PsiElement): List<GotoRelatedItem> {
        val file = psiElement.containingFile ?: return emptyList()
        val name = file.name
        val counterpartName = when {
            name.endsWith(".res") -> name.removeSuffix(".res") + ".resi"
            name.endsWith(".resi") -> name.removeSuffix(".resi") + ".res"
            else -> return emptyList()
        }
        val counterpart = file.containingDirectory?.findFile(counterpartName) ?: return emptyList()
        return listOf(GotoRelatedItem(counterpart))
    }
}
