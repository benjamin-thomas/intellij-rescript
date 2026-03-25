package com.github.benjamin_thomas.intellij_rescript

import com.intellij.openapi.util.IconLoader

interface Icons {
    companion object {
        val ReScript = IconLoader.getIcon("icons/rescript.svg", Icons::class.java)
        val ReScriptInterface = IconLoader.getIcon("icons/rescript-interface.svg", Icons::class.java)
    }
}
