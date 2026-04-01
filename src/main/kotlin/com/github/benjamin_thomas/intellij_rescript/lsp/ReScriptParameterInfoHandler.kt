package com.github.benjamin_thomas.intellij_rescript.lsp

import com.intellij.lang.parameterInfo.*
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.redhat.devtools.lsp4ij.LanguageServerItem
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor
import com.redhat.devtools.lsp4ij.LSPIJUtils
import com.redhat.devtools.lsp4ij.features.signatureHelp.LSPSignatureHelperPsiElement
import org.eclipse.lsp4j.*

/**
 * Custom parameter info handler that shows the full signature from the LSP
 * signatureHelp response.
 *
 * LSP4IJ's built-in LSPParameterInfoHandler truncates the signature label
 * (e.g., drops the closing paren and return type). This handler renders the
 * complete label as returned by the server.
 */
class ReScriptParameterInfoHandler : ParameterInfoHandler<LSPSignatureHelperPsiElement, SignatureInformation> {
    private val logger = Logger.getInstance("ReScript")

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): LSPSignatureHelperPsiElement? {
        val textRange = getTextRange(context) ?: return null
        return LSPSignatureHelperPsiElement(context.file, textRange)
    }

    override fun showParameterInfo(element: LSPSignatureHelperPsiElement, context: CreateParameterInfoContext) {
        val file = element.containingFile
        val position = LSPIJUtils.toPosition(context.offset, context.editor.document)
        val params = SignatureHelpParams(
            TextDocumentIdentifier(LSPIJUtils.toUri(file).toString()),
            position,
            SignatureHelpContext().apply { triggerKind = SignatureHelpTriggerKind.Invoked },
        )

        // LSP4IJ marks all its APIs as @ApiStatus.Internal (no stable public API yet).
        // LanguageServiceAccessor is the standard way to access language servers.
        @Suppress("UnstableApiUsage")
        LanguageServiceAccessor.getInstance(file.project)
            .getLanguageServers(file, null, null)
            .thenAccept { servers ->
                val server = findReScriptLanguageServer(servers)
                if (server == null) {
                    notifyMissingReScriptServer(file.project)
                    logger.error("ReScript signatureHelp failed: rescriptLanguageServer not found in matching servers")
                    return@thenAccept
                }

                // Non-blocking: continue when the LSP reply arrives instead of calling .get().
                server.textDocumentService.signatureHelp(params)
                    .thenAccept { signatureHelp ->
                        if (signatureHelp != null && !signatureHelp.signatures.isNullOrEmpty()) {
                            // Parameter info UI must be updated on IntelliJ's EDT.
                            ApplicationManager.getApplication().invokeLater {
                                element.activeSignatureHelp = signatureHelp
                                context.itemsToShow = signatureHelp.signatures.toTypedArray()
                                context.showHint(element, context.offset, this)
                            }
                        }
                    }
                    .whenComplete { _, throwable ->
                        if (throwable != null) {
                            logger.warn("signatureHelp request failed", throwable)
                        }
                    }
            }
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): LSPSignatureHelperPsiElement? {
        val textRange = getTextRange(context) ?: return null
        return LSPSignatureHelperPsiElement(context.file, textRange)
    }

    override fun updateParameterInfo(element: LSPSignatureHelperPsiElement, context: UpdateParameterInfoContext) {
        val signatureHelp = element.activeSignatureHelp ?: return
        context.setCurrentParameter(signatureHelp.activeParameter ?: 0)
    }

    override fun updateUI(signatureInfo: SignatureInformation?, context: ParameterInfoUIContext) {
        if (signatureInfo == null) {
            context.isUIComponentEnabled = false
            return
        }

        val label = signatureInfo.label ?: ""

        // Highlight the active parameter
        val activeParam = context.currentParameterIndex
        val params = signatureInfo.parameters
        var highlightStart = -1
        var highlightEnd = -1

        if (params != null && activeParam >= 0 && activeParam < params.size) {
            val paramLabel = params[activeParam].label
            if (paramLabel != null && paramLabel.isLeft) {
                val paramStr = paramLabel.left
                val idx = label.indexOf(paramStr)
                if (idx >= 0) {
                    highlightStart = idx
                    highlightEnd = idx + paramStr.length
                }
            } else if (paramLabel != null && paramLabel.isRight) {
                val range = paramLabel.right
                highlightStart = range.first
                highlightEnd = range.second
            }
        }

        context.setupUIComponentPresentation(
            label,
            highlightStart,
            highlightEnd,
            false,
            false,
            false,
            context.defaultParameterColor,
        )
    }

    private fun getTextRange(context: ParameterInfoContext): TextRange? {
        val offset = context.offset
        if (offset < 0) return null
        return TextRange(offset, offset)
    }

    private fun findReScriptLanguageServer(servers: List<LanguageServerItem>): LanguageServerItem? =
        servers.firstOrNull { it.serverDefinition.id == "rescriptLanguageServer" }

    private fun notifyMissingReScriptServer(project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReScript")
            .createNotification(
                "ReScript signature help unavailable",
                "ReScript language server not found for this file.",
                NotificationType.ERROR
            )
            .notify(project)
    }
}
