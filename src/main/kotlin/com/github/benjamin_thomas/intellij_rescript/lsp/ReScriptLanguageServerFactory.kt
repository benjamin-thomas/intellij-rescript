package com.github.benjamin_thomas.intellij_rescript.lsp

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.LanguageServerManager
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import java.io.File
import java.nio.file.Path
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
        val settings = service<ReScriptLspSettings>().state
        val configuredNode = normalizedPath(settings.nodePath)
        val configuredServer = normalizedPath(settings.languageServerPath)
        val nodePath = executablePath(configuredNode)
        val serverPath = executablePath(configuredServer)

        if (nodePath == null || serverPath == null) {
            notifyPathsNotFound(project, configuredNode, nodePath, configuredServer, serverPath)
            LanguageServerManager.getInstance(project).stop("rescriptLanguageServer")
            return
        }

        commands = listOf(nodePath, serverPath, "--stdio")
        workingDirectory = project.basePath
    }

    private fun notifyPathsNotFound(
        project: Project,
        configuredNode: String?,
        resolvedNode: String?,
        configuredServer: String?,
        resolvedServer: String?,
    ) {
        if (!notifiedMissing.compareAndSet(false, true)) return

        val message = buildString {
            if (resolvedNode == null) {
                append("<b>Node path</b> is not set or not executable.")
                configuredNode?.let {
                    append("<br>Configured path: <code>")
                    append(escapeHtml(it))
                    append("</code>")
                }
                append("<br><br>")
            }
            if (resolvedServer == null) {
                append("<b>Language server path</b> is not set or not executable.")
                configuredServer?.let {
                    append("<br>Configured path: <code>")
                    append(escapeHtml(it))
                    append("</code>")
                }
                append("<br><br>")
            }
            append("ReScript needs explicit absolute paths to both <code>node</code> and <code>rescript-language-server</code>.")
            append("<br><br>")
            append("Install the language server:")
            append("<br><br>")
            append("<code>npm install -g @rescript/language-server</code>")
            append("<br><br>")
            append("Then set the executable paths in Languages &amp; Frameworks &gt; ReScript.")
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReScript")
            .createNotification(
                "ReScript LSP not started",
                message,
                NotificationType.ERROR
            )
            .addAction(NotificationAction.createSimple("Open ReScript Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ReScriptLspConfigurable::class.java)
            })
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

private const val RESCRIPT_LANGUAGE_SERVER = "rescript-language-server"
private const val NODE_BINARY = "node"

fun autoDetectLanguageServerPath(
    pathEnv: String = System.getenv("PATH") ?: "",
    extraSearchDirs: Sequence<Path> = defaultLanguageServerSearchDirs(),
): String? =
    firstExecutableIn(RESCRIPT_LANGUAGE_SERVER, pathEnv, extraSearchDirs)

fun autoDetectNodePath(
    pathEnv: String = System.getenv("PATH") ?: "",
    extraSearchDirs: Sequence<Path> = defaultLanguageServerSearchDirs(),
): String? =
    firstExecutableIn(NODE_BINARY, pathEnv, extraSearchDirs)

private fun firstExecutableIn(
    executableName: String,
    pathEnv: String,
    extraSearchDirs: Sequence<Path>,
): String? {
    val searchDirs = sequenceOf(pathDirs(pathEnv), extraSearchDirs).flatten().distinct()
    return searchDirs.firstNotNullOfOrNull { dir ->
        executablePath(dir.resolve(executableName).toString())
    }
}

private fun pathDirs(pathEnv: String): Sequence<Path> =
    pathEnv.splitToSequence(File.pathSeparator)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { Path.of(it) }

private fun defaultLanguageServerSearchDirs(): Sequence<Path> {
    val home = System.getProperty("user.home")
    // GUI-launched JetBrains instances may not inherit the shell PATH, so we
    // probe a few conventional install locations directly.
    return sequenceOf(
        Path.of("/usr/local/bin"),
        Path.of("/opt/homebrew/bin"),
        Path.of("/usr/bin"),
        Path.of(home, ".nvm", "versions", "node"),
    ).flatMap { path ->
        if (path.fileName?.toString() == "node") {
            runCatching {
                java.nio.file.Files.list(path).use { children ->
                    children.map { it.resolve("bin") }.toList().asSequence()
                }
            }.getOrDefault(emptySequence())
        } else {
            sequenceOf(path)
        }
    }
}

fun normalizedPath(path: String?): String? =
    path?.trim()?.ifBlank { null }

fun executablePath(path: String?): String? =
    normalizedPath(path)
        ?.let(::File)
        ?.takeIf { it.isFile && it.canExecute() }
        ?.absolutePath

fun restartReScriptLspInOpenProjects(shouldStart: Boolean) {
    ProjectManager.getInstance().openProjects.forEach { project ->
        val manager = LanguageServerManager.getInstance(project)
        manager.stop("rescriptLanguageServer")
        if (shouldStart) {
            manager.start("rescriptLanguageServer")
        }
    }
}

private fun escapeHtml(text: String): String =
    buildString(text.length) {
        text.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(char)
            }
        }
    }
