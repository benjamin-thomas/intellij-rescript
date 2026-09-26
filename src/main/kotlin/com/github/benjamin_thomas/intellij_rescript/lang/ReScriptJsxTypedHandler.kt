package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.ReScriptFile
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptJsxElement
import com.github.benjamin_thomas.intellij_rescript.lang.psi.ReScriptJsxFragment
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet

class ReScriptJsxTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '>' || file !is ReScriptFile) return Result.CONTINUE

        val caret = editor.caretModel.offset
        // The typed char is not in the PSI tree until the document is committed
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val gt = file.findElementAt(caret - 1) ?: return Result.CONTINUE
        val close = jsxAutoCloseText(gt) ?: return Result.CONTINUE

        // Not wrapped in its own command: joining the typing command keeps it one undo step
        ApplicationManager.getApplication().runWriteAction {
            editor.document.insertString(caret, close)
            editor.caretModel.moveToOffset(caret)
        }
        return Result.STOP
    }
}

/**
 * What to insert after a typed `>` that became the leaf [gt] — or null for
 * "do nothing". Pure query so the branch matrix is testable without an editor.
 */
fun jsxAutoCloseText(gt: PsiElement): String? {
    if (gt.node.elementType != ReScriptTypes.JSX_GT) return null
    return when (val parent = gt.parent) {
        is ReScriptJsxElement -> elementCloseText(parent)
        is ReScriptJsxFragment -> fragmentCloseText(parent, gt)
        else -> null
    }
}

// A closing tag's `>` lives under JsxClosingTag; an opening tag's is a direct
// child of JsxElement. Only the latter completes a tag.
private fun elementCloseText(element: ReScriptJsxElement): String? {
    val tag = element.jsxTagName?.text ?: return null
    // Retyping the opening `>` of an element that already has its closing tag
    if (element.jsxClosingTag != null && !tookParentsClosingTag(element)) return null
    return "</$tag>"
}

// The parser hands a closing tag to the innermost open element, whatever its
// name, so a tag typed inside a parent takes the parent's closing tag, the
// parent takes its own parent's, and so on up to an ancestor left without one.
private fun tookParentsClosingTag(jsx: PsiElement): Boolean = when (val parent = jsx.parent) {
    is ReScriptJsxElement -> parent.jsxClosingTag == null || tookParentsClosingTag(parent)
    is ReScriptJsxFragment -> !hasClosingTag(parent) || tookParentsClosingTag(parent)
    else -> false
}

// The opening `>` of a fragment directly follows `<`; the closing one follows `</`
private fun fragmentCloseText(fragment: ReScriptJsxFragment, gt: PsiElement): String? {
    if (gt.prevSibling?.node?.elementType != ReScriptTypes.JSX_LT) return null
    // Retyped the opening `>` of a fragment that already has its `</>`
    if (hasClosingTag(fragment) && !tookParentsClosingTag(fragment)) return null
    return "</>"
}

private fun hasClosingTag(fragment: ReScriptJsxFragment): Boolean =
    fragment.node.getChildren(TokenSet.create(ReScriptTypes.JSX_LT_SLASH)).isNotEmpty()
