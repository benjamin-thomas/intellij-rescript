package com.github.benjamin_thomas.intellij_rescript

import com.intellij.openapi.fileTypes.LanguageFileType

class ReScriptInterfaceFileType : LanguageFileType(ReScriptLanguage.INSTANCE) {
    companion object {
        val INSTANCE = ReScriptInterfaceFileType()
    }

    override fun getName(): String = "ReScript Interface"

    override fun getDescription(): String = "ReScript interface file"

    override fun getDefaultExtension(): String = "resi"

    override fun getIcon() = Icons.ReScriptInterface
}
