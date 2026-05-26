package com.github.xepozz.infection.inspection

import com.github.xepozz.infection.InfectionBundle
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class MarkInfectionIgnoreFix(private val mutatorCode: String) : LocalQuickFix {

    override fun getFamilyName(): String =
        InfectionBundle.message("inspection.escapedMutant.quickFix.markIgnore.familyName")

    override fun getName(): String =
        InfectionBundle.message("inspection.escapedMutant.quickFix.markIgnore.name", mutatorCode)

    override fun startInWriteAction(): Boolean = true

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile ?: return
        if (!file.isWritable) return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val lineNumber = document.getLineNumber(element.textRange.startOffset)
        val lineStart = document.getLineStartOffset(lineNumber)

        WriteCommandAction.runWriteCommandAction(project, name, null, {
            document.insertString(lineStart, "/** @infection-ignore-all analysis:$mutatorCode */\n")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }, file)
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
        return IntentionPreviewInfo.CustomDiff(
            previewDescriptor.psiElement.containingFile.fileType,
            "",
            "/** @infection-ignore-all analysis:$mutatorCode */\n"
        )
    }
}
