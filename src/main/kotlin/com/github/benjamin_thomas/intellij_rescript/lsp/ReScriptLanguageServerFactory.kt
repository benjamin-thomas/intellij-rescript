package com.github.benjamin_thomas.intellij_rescript.lsp

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ReScriptLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider =
        ReScriptLanguageServer(project)

    override fun createLanguageClient(project: Project): LanguageClientImpl =
        ReScriptLanguageClient(project)

    override fun createClientFeatures(): com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures =
        com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures()
            .setSemanticTokensFeature(ReScriptSemanticTokensFeature())
}

/**
 * Disables LSP semantic tokens for ReScript.
 *
 * LSP4IJ's default semantic token styling inherits from IntelliJ's "Language
 * Defaults" which underlines reassigned variables. This makes most ReScript
 * variables appear underlined — very distracting. Since our lexer-based syntax
 * highlighting is already good enough, we disable semantic tokens entirely.
 *
 * Can be re-enabled in the future with custom color mappings that don't inherit
 * the underline effect.
 */
class ReScriptSemanticTokensFeature : com.redhat.devtools.lsp4ij.client.features.LSPSemanticTokensFeature() {
    override fun isSupported(file: com.intellij.psi.PsiFile): Boolean = false
}

/**
 * Custom language client that sends ReScript-specific configuration to the
 * LSP server when it asks via workspace/configuration.
 *
 * The ReScript LSP server disables several features by default (inlay hints,
 * code lens) and shows a "start build?" notification unless configured not to.
 * This client responds with our preferred settings.
 *
 * See rescript-vscode/server/src/config.ts for all available options.
 */
class ReScriptLanguageClient(project: Project) : LanguageClientImpl(project) {
    override fun createSettings(): Any = rescriptSettings()

    companion object {
        /**
         * ReScript LSP server configuration.
         * Used both in initializationOptions (during initialize) and in
         * workspace/configuration responses (when server pulls config later).
         *
         * See rescript-vscode/server/src/config.ts for all available options.
         */
        fun rescriptSettings(): JsonObject = JsonObject().apply {
            // Don't prompt — we expect the user to run `rescript build -w` (or
            // `rescript watch` for v12+) in a terminal themselves. This way they
            // see compiler output directly. Without a running watcher, LSP features
            // like code lens, hover, and go-to-def work on stale .cmt files.
            add("askToStartBuild", JsonPrimitive(false))

            // Inlay hints disabled — too noisy (shows `: int` on trivially typed bindings,
            // even when the user already added a type annotation manually).
            // Code lens provides the same type info less intrusively.
            add("inlayHints", JsonObject().apply {
                add("enable", JsonPrimitive(false))
                add("maxLength", JsonPrimitive(25))
            })

            // Enable code lens
            add("codeLens", JsonPrimitive(true))

            // Enable signature help (parameter info)
            add("signatureHelp", JsonObject().apply {
                add("enabled", JsonPrimitive(true))
                add("forConstructorPayloads", JsonPrimitive(true))
            })

            // incrementalTypechecking is only relevant when the server spawns its
            // own build watcher (askToStartBuild=true). Since we expect the user
            // to run the watcher themselves, this setting has no effect.
        }
    }
}

class ReScriptLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    // LSP4IJ retries the connection multiple times, so we guard the notification to avoid spamming the user.
    private val notifiedMissing = AtomicBoolean(false)

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

    private fun notifyServerNotFound(project: Project) {
        if (!notifiedMissing.compareAndSet(false, true)) return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReScript")
            .createNotification(
                "ReScript LSP not found",
                """
                    Install it with:
                    <br><br>
                    <code>npm install -g @rescript/language-server</code>
                    <br><br>
                    After installing, you may need to log out and back in
                    (or relaunch your IDE from a terminal to pick up the updated PATH).
                """.trimIndent(),
                NotificationType.ERROR
            )
            .notify(project)
    }

    /**
     * Sent to the server as initializationOptions during the initialize request.
     * The ReScript server reads this before any files are opened — critical for
     * settings like askToStartBuild that fire on first didOpen.
     *
     * See rescript-vscode/server/src/server.ts line ~1650:
     *   let initialConfiguration = initParams.initializationOptions?.extensionConfiguration
     */
    override fun getInitializationOptions(rootUri: com.intellij.openapi.vfs.VirtualFile?): Any {
        return JsonObject().apply {
            add("extensionConfiguration", ReScriptLanguageClient.rescriptSettings())
        }
    }
}

// Finds rescript-language-server in PATH.
// Installed globally via: npm install -g @rescript/language-server
fun findServerPath(): String? =
    System.getenv("PATH")?.split(File.pathSeparator)?.firstNotNullOfOrNull { dir ->
        File(dir, "rescript-language-server").takeIf { it.canExecute() }?.path
    }
