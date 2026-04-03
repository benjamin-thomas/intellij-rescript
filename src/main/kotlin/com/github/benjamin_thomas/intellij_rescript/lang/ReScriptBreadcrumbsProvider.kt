package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import com.github.benjamin_thomas.intellij_rescript.ReScriptLanguage
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptLetBinding
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptModuleBinding

class ReScriptBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> = arrayOf(ReScriptLanguage)

    override fun acceptElement(element: PsiElement): Boolean =
        (element is ReScriptLetBinding || element is ReScriptModuleBinding)
            && element.name != null

    override fun getElementInfo(element: PsiElement): String {
        val name = (element as? PsiNameIdentifierOwner)?.name ?: return ""
        return when (element) {
            is ReScriptLetBinding -> "let $name"
            is ReScriptModuleBinding -> "module $name"
            else -> name
        }
    }
}
