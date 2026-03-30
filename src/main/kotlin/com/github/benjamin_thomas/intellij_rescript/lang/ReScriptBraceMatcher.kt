package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class ReScriptBraceMatcher : PairedBraceMatcher {

    companion object {
        private val PAIRS = arrayOf(
            BracePair(ReScriptTypes.LBRACE, ReScriptTypes.RBRACE, true),
            BracePair(ReScriptTypes.LPAREN, ReScriptTypes.RPAREN, false),
            BracePair(ReScriptTypes.LBRACKET, ReScriptTypes.RBRACKET, false),
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS

    // Always auto-insert the closing brace. See Javadoc on PairedBraceMatcher for details.
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    // Don't shift the highlight — just highlight the opening brace itself.
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
