package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

const val RESCRIPT_LSP_PATH_PROPERTY = "rescript.lsp.path"

@Service(Service.Level.APP)
@State(name = "ReScriptLspSettings", storages = [Storage("rescript.xml")])
class ReScriptLspSettings : PersistentStateComponent<ReScriptLspSettings.State> {
    data class State(
        var languageServerPath: String? = null,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun seedFromLaunchProperty() {
        state.languageServerPath = seededLanguageServerPath(
            currentPath = state.languageServerPath,
            launchProperty = System.getProperty(RESCRIPT_LSP_PATH_PROPERTY),
        )
    }
}

fun seededLanguageServerPath(currentPath: String?, launchProperty: String?): String? {
    val normalizedCurrentPath = normalizedPath(currentPath)
    if (normalizedCurrentPath != null) return normalizedCurrentPath

    return normalizedPath(launchProperty)
}
