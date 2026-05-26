package com.github.xepozz.infection.inspection

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.results.MutantAnchor
import com.github.xepozz.infection.results.MutantRecord
import com.github.xepozz.infection.results.MutantStatus
import com.github.xepozz.infection.results.MutationResultsService
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement

class EscapedMutantInspection : LocalInspectionTool() {

    override fun getShortName(): String = SHORT_NAME

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val virtualFile = holder.file.virtualFile ?: return PsiElementVisitor.EMPTY_VISITOR
        val service = MutationResultsService.getInstance(holder.project)
        val records = service.getResultsFor(virtualFile.path)
            .filter { it.status == MutantStatus.ESCAPED }
        if (records.isEmpty()) return PsiElementVisitor.EMPTY_VISITOR

        val fileText = holder.file.text
        // Deduplicate by (mutationId, anchor) — defends against the same mutant being saved twice
        // and against a single mutation matching multiple PSI visits.
        val seen = HashSet<String>()
        val anchored: List<Pair<MutantRecord, MutantAnchor.Anchor>> = records.mapNotNull { record ->
            val anchor = MutantAnchor.reanchor(record, fileText) ?: return@mapNotNull null
            val key = "${record.mutationId}@${anchor.startOffset}-${anchor.endOffsetExclusive}-${record.mutatorName}"
            if (!seen.add(key)) return@mapNotNull null
            record to anchor
        }
        if (anchored.isEmpty()) return PsiElementVisitor.EMPTY_VISITOR

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is LeafPsiElement) return
                val elementRange = element.textRange
                val matching = anchored.filter {
                    it.second.startOffset in elementRange.startOffset until elementRange.endOffset
                }
                if (matching.isEmpty()) return
                matching.forEach { (record, anchor) ->
                    val end = anchor.endOffsetExclusive.coerceAtMost(fileText.length)
                    val start = anchor.startOffset.coerceAtLeast(elementRange.startOffset)
                    val clampedEnd = end.coerceAtMost(elementRange.endOffset)
                    if (clampedEnd <= start) return@forEach
                    val highlight = TextRange(start, clampedEnd).shiftRight(-elementRange.startOffset)
                    holder.registerProblem(
                        element,
                        InfectionBundle.message("inspection.escapedMutant.message", record.mutatorName),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        highlight,
                        MarkInfectionIgnoreFix(record.mutatorName),
                        ShowMutantDiffFix(record),
                    )
                }
            }
        }
    }

    companion object {
        const val SHORT_NAME = "InfectionEscapedMutant"
    }
}
