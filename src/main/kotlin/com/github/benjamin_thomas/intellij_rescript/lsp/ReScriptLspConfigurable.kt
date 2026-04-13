package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ReScriptLspConfigurable(private val project: Project) : Configurable, Configurable.NoScroll {
    private val settings = service<ReScriptLspSettings>()
    private val nodePathField = JBTextField()
    private val languageServerPathField = JBTextField()

    override fun getDisplayName(): String = "ReScript"

    override fun createComponent(): JComponent {
        val nodePanel = JPanel(BorderLayout(8, 0)).apply {
            add(nodePathField, BorderLayout.CENTER)
            add(JButton("Auto-detect").apply {
                addActionListener {
                    nodePathField.text = autoDetectNodePath().orEmpty()
                }
            }, BorderLayout.EAST)
        }

        val serverPanel = JPanel(BorderLayout(8, 0)).apply {
            add(languageServerPathField, BorderLayout.CENTER)
            add(JButton("Auto-detect").apply {
                addActionListener {
                    languageServerPathField.text = autoDetectLanguageServerPath().orEmpty()
                }
            }, BorderLayout.EAST)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("Node path:", nodePanel, 1, false)
            .addComponent(
                JLabel("Configure an absolute path to the node executable (e.g. /usr/local/bin/node)."),
                1,
            )
            .addVerticalGap(12)
            .addLabeledComponent("Language server path:", serverPanel, 1, false)
            .addComponent(
                JLabel("Configure an absolute path to the globally installed rescript-language-server executable."),
                1,
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(16)
            add(form, BorderLayout.CENTER)
        }
    }

    override fun isModified(): Boolean =
        normalizedPath(nodePathField.text) != normalizedPath(settings.state.nodePath) ||
            normalizedPath(languageServerPathField.text) != normalizedPath(settings.state.languageServerPath)

    override fun apply() {
        val configuredNode = normalizedPath(nodePathField.text)
        val configuredServer = normalizedPath(languageServerPathField.text)

        if (configuredNode != null && executablePath(configuredNode) == null) {
            throw ConfigurationException("Node path is not executable: $configuredNode")
        }
        if (configuredServer != null && executablePath(configuredServer) == null) {
            throw ConfigurationException("ReScript language server path is not executable: $configuredServer")
        }

        settings.state.nodePath = configuredNode
        settings.state.languageServerPath = configuredServer

        val canStart = configuredNode != null && configuredServer != null
        restartReScriptLspInOpenProjects(shouldStart = canStart)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReScript")
            .createNotification(
                "ReScript LSP settings updated",
                if (canStart) {
                    "The ReScript language server has been restarted with the configured paths."
                } else {
                    "The ReScript language server has been stopped because the node and language server paths must both be configured."
                },
                NotificationType.INFORMATION,
            )
            .notify(project)
    }

    override fun reset() {
        nodePathField.text = settings.state.nodePath.orEmpty()
        languageServerPathField.text = settings.state.languageServerPath.orEmpty()
    }
}
