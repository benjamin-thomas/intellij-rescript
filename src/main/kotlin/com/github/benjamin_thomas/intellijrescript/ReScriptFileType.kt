package com.github.benjamin_thomas.intellijrescript

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class ReScriptFileType : LanguageFileType(ReScriptLanguage.INSTANCE) {
    companion object {
        val INSTANCE = ReScriptFileType()
    }

    override fun getName(): String = "ReScript"

    override fun getDescription(): String = "ReScript language file"

    override fun getDefaultExtension(): String = "res"

    override fun getIcon(): Icon? = null
}
