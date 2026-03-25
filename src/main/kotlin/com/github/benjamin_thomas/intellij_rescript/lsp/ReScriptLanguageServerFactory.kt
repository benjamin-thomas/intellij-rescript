package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ReScriptLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider =
        ReScriptLanguageServer(project)
}

// LSP4IJ retries the connection multiple times, so we guard the notification
// to avoid spamming the user.
private val notifiedMissing = AtomicBoolean(false)

private fun notifyServerNotFound(project: Project) {
    if (!notifiedMissing.compareAndSet(false, true)) return

    NotificationGroupManager.getInstance()
        .getNotificationGroup("ReScript")
        .createNotification(
            "ReScript LSP not found",
            "Install it with: npm install -g @rescript/language-server",
            NotificationType.ERROR
        )
        .notify(project)
}

class ReScriptLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    init {
        setup(project)
    }

    private fun setup(project: Project) {
        val serverPath = findServerPath()
        if (serverPath == null) {
            notifyServerNotFound(project)
            LanguageServerManager.getInstance(project).stop("rescriptLanguageServer")
            return
        }
        commands = listOf(serverPath, "--stdio")
        workingDirectory = project.basePath
    }
}

// Finds rescript-language-server in PATH.
// Installed globally via: npm install -g @rescript/language-server
fun findServerPath(): String? =
    System.getenv("PATH")?.split(File.pathSeparator)?.firstNotNullOfOrNull { dir ->
        File(dir, "rescript-language-server").takeIf { it.canExecute() }?.path
    }
