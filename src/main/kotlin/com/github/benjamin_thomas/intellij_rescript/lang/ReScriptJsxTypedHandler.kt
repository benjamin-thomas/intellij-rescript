package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.ReScriptFile
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptJsxElement
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class ReScriptJsxTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '>' || file !is ReScriptFile) return Result.CONTINUE

        // The typed char is not in the PSI tree until the document is committed
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        
        
        val caret = editor.caretModel.offset
        val gt = file.findElementAt(caret - 1) ?: return Result.CONTINUE
        if (gt.node.elementType != ReScriptTypes.JSX_GT) return Result.CONTINUE

        // A closing tag's `>` lives under JsxClosingTag; an opening tag's is a
        // direct child of JsxElement. Only the latter completes a tag.
        val element = gt.parent as? ReScriptJsxElement ?: return Result.CONTINUE
        val tag = element.jsxTagName?.text ?: return Result.CONTINUE

        val document = editor.document
        if (document.text.startsWith("</$tag", caret)) return Result.CONTINUE

        // Not wrapped in its own command: joining the typing command keeps it one undo step
        ApplicationManager.getApplication().runWriteAction {
            document.insertString(caret, "</$tag>")
            editor.caretModel.moveToOffset(caret)
        }
        return Result.STOP
    }
}
