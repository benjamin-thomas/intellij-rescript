package com.github.benjamin_thomas.intellij_rescript

import com.intellij.lang.Language

object ReScriptLanguage : Language("ReScript") {
    @Suppress("unused")
    private fun readResolve(): Any = ReScriptLanguage
}
