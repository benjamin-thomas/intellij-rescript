package com.github.benjamin_thomas.intellij_rescript

import com.intellij.openapi.fileTypes.LanguageFileType

object ReScriptInterfaceFileType : LanguageFileType(ReScriptLanguage) {
    override fun getName(): String = "ReScript Interface"
    override fun getDescription(): String = "ReScript interface file"
    override fun getDefaultExtension(): String = "resi"
    override fun getIcon() = Icons.ReScriptInterface
}
