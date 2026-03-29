package com.github.benjamin_thomas.intellij_rescript.lang

import com.intellij.lang.Commenter

class ReScriptCommenter : Commenter {
    override fun getLineCommentPrefix() = "// "
    override fun getBlockCommentPrefix() = "/* "
    override fun getBlockCommentSuffix() = " */"
    
    // These define how to escape /* and */ inside block-commented code.
    // Languages like Java need this (e.g. /* becomes /(*) ) because their block comments
    // don't nest. ReScript block comments nest natively, so no escaping is needed.
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
