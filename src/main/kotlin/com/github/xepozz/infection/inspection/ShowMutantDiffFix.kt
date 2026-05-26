package com.github.xepozz.infection.inspection

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.results.MutantDiffViewer
import com.github.xepozz.infection.results.MutantRecord
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

class ShowMutantDiffFix(private val record: MutantRecord) : LocalQuickFix {

    override fun getFamilyName(): String =
        InfectionBundle.message("inspection.escapedMutant.quickFix.showDiff.familyName")

    override fun getName(): String =
        InfectionBundle.message("inspection.escapedMutant.quickFix.showDiff.name", record.mutatorName)

    override fun startInWriteAction(): Boolean = false

    override fun availableInBatchMode(): Boolean = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val hintFile = descriptor.psiElement?.containingFile?.virtualFile
        MutantDiffViewer.show(project, record, hintFile)
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
        val original = record.originalSnippet ?: return IntentionPreviewInfo.EMPTY
        val mutated = record.mutatedSnippet ?: return IntentionPreviewInfo.EMPTY
        val fileType = previewDescriptor.psiElement.containingFile.fileType
        return IntentionPreviewInfo.CustomDiff(fileType, original, mutated)
    }
}
