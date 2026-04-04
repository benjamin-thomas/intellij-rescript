package com.github.benjamin_thomas.intellij_rescript.lang

import com.github.benjamin_thomas.intellij_rescript.ReScriptLanguage
import com.intellij.codeInsight.daemon.impl.actions.AbstractBatchSuppressByNoInspectionCommentFix
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiWhiteSpace

class ReScriptInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        val declaration = findTopLevelDeclaration(element) ?: return false
        return leadingComments(declaration).any { comment ->
            val matcher = SuppressionUtil.SUPPRESS_IN_LINE_COMMENT_PATTERN.matcher(comment.text)
            matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(matcher.group(1), toolId)
        }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> = arrayOf(
        SuppressInspectionFix(toolId),
        SuppressInspectionFix(SuppressionUtil.ALL),
    )

    private fun findTopLevelDeclaration(element: PsiElement): PsiElement? {
        return generateSequence(element) { it.parent }
            .firstOrNull { it.parent is PsiFile }
    }

    private fun leadingComments(element: PsiElement): Sequence<PsiComment> {
        return generateSequence(element.prevSibling) { it.prevSibling }
            .takeWhile { it is PsiWhiteSpace || it is PsiComment }
            .filterIsInstance<PsiComment>()
    }

    private class SuppressInspectionFix(
        id: String,
    ) : AbstractBatchSuppressByNoInspectionCommentFix(id, id == SuppressionUtil.ALL) {
        init {
            text = if (id == SuppressionUtil.ALL) {
                "Suppress all inspections for declaration"
            } else {
                "Suppress for declaration with comment"
            }
        }

        override fun getContainer(context: PsiElement?): PsiElement? {
            if (context == null) return null
            return generateSequence(context) { it.parent }
                .firstOrNull { it.parent is PsiFile }
        }

        override fun createSuppression(project: Project, element: PsiElement, container: PsiElement) {
            val comment = SuppressionUtil.createComment(project, "noinspection $myID", ReScriptLanguage)
            val newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")
            val parent = container.parent
            parent.addBefore(comment, container)
            parent.addBefore(newline, container)
        }
    }
}
