package com.github.benjamin_thomas.intellij_rescript

import com.intellij.openapi.fileTypes.LanguageFileType

/**
 * ReScript interface files (.resi) declare the public API of a module,
 * hiding internal implementation details. Same concept as .mli in OCaml.
 */
object ReScriptInterfaceFileType : LanguageFileType(ReScriptLanguage) {
    override fun getName(): String = "ReScript Interface"
    override fun getDisplayName(): String = "ReScript Interface"
    override fun getDescription(): String = "ReScript interface file"
    override fun getDefaultExtension(): String = "resi"
    override fun getIcon() = Icons.ReScriptInterface
}
