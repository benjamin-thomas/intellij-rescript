package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.ReScriptLanguage
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

class ReScriptSpellcheckingStrategy : SpellcheckingStrategy() {

    override fun isMyContext(element: PsiElement): Boolean =
        ReScriptLanguage.`is`(element.language)

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        return when (element.node.elementType) {
            ReScriptTypes.STRING_CONTENT, ReScriptTypes.TEMPLATE_CONTENT -> TEXT_TOKENIZER
            else -> super.getTokenizer(element)
        }
    }
}
