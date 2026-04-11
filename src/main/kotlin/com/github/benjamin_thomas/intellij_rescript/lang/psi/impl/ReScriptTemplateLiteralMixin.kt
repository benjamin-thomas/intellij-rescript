package com.github.benjamin_thomas.intellij_rescript.lang.psi.impl

import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptTemplateLiteral
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

abstract class ReScriptTemplateLiteralMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost {

    override fun isValidHost(): Boolean =
        (this as ReScriptTemplateLiteral).templateInterpolationList.isEmpty()

    override fun updateText(text: String): PsiLanguageInjectionHost {
        // TODO: implement when rename/refactoring needs it
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> {
        return object : LiteralTextEscaper<ReScriptTemplateLiteralMixin>(this) {
            override fun isOneLine(): Boolean = false

            override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
                outChars.append(rangeInsideHost.substring(myHost.text))
                return true
            }

            override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
                return rangeInsideHost.startOffset + offsetInDecoded
            }
        }
    }
}
