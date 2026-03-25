package com.github.benjamin_thomas.intellij_rescript

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class ReScriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ReScriptLanguage) {
    override fun getFileType() = ReScriptFileType
    override fun toString() = "ReScript File"
}
