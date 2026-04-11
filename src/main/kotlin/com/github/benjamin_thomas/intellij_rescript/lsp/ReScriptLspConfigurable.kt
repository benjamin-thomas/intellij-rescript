package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ReScriptLspConfigurable(private val project: Project) : Configurable, Configurable.NoScroll {
    private val settings = service<ReScriptLspSettings>()
    private val languageServerPathField = JBTextField()

    override fun getDisplayName(): String = "ReScript"

    override fun createComponent(): JComponent {
        val pathPanel = JPanel(BorderLayout(8, 0)).apply {
            add(languageServerPathField, BorderLayout.CENTER)
            add(JButton("Auto-detect").apply {
                addActionListener {
                    languageServerPathField.text = autoDetectLanguageServerPath().orEmpty()
                }
            }, BorderLayout.EAST)
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Language server path:", pathPanel, 1, false)
            .addComponent(
                JLabel("Configure an absolute path to the globally installed rescript-language-server executable."),
                1,
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        normalizedPath(languageServerPathField.text) != normalizedPath(settings.state.languageServerPath)

    override fun apply() {
        val configuredPath = normalizedPath(languageServerPathField.text)
        if (configuredPath != null && executablePath(configuredPath) == null) {
            throw ConfigurationException("ReScript language server path is not executable: $configuredPath")
        }

        settings.state.languageServerPath = configuredPath
        restartReScriptLspInOpenProjects(shouldStart = configuredPath != null)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReScript")
            .createNotification(
                "ReScript LSP settings updated",
                if (configuredPath != null) {
                    "The ReScript language server has been restarted with the configured path."
                } else {
                    "The ReScript language server has been stopped because no path is configured."
                },
                NotificationType.INFORMATION,
            )
            .notify(project)
    }

    override fun reset() {
        languageServerPathField.text = settings.state.languageServerPath.orEmpty()
    }
}
