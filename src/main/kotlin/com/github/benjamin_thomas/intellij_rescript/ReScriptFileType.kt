package com.github.benjamin_thomas.intellij_rescript

import com.intellij.openapi.fileTypes.LanguageFileType

object ReScriptFileType : LanguageFileType(ReScriptLanguage) {
    override fun getName(): String = "ReScript"
    override fun getDisplayName(): String = "ReScript"
    override fun getDescription(): String = "ReScript language file"
    override fun getDefaultExtension(): String = "res"
    override fun getIcon() = Icons.ReScript
}
