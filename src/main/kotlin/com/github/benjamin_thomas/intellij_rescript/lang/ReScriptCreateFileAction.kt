package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.Icons.Companion.ReScript
import com.github.benjamin_thomas.intellij_rescript.Icons.Companion.ReScriptInterface
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class ReScriptCreateFileAction : CreateFileFromTemplateAction(
    "ReScript File", "Create a new ReScript file", ReScript
), DumbAware {

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New ReScript File")
            .addKind("Module", ReScript, "ReScript Module")
            .addKind("Interface", ReScriptInterface, "ReScript Interface")
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String =
        "Create ReScript File $newName"
}
