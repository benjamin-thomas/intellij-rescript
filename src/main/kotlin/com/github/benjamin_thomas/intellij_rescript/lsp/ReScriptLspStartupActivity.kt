package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ReScriptLspStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        service<ReScriptLspSettings>().seedFromLaunchProperty()
    }
}
