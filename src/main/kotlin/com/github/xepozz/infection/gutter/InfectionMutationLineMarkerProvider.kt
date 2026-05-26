package com.github.xepozz.infection.gutter

import com.github.xepozz.infection.InfectionIcons
import com.github.xepozz.infection.results.MutantAnchor
import com.github.xepozz.infection.results.MutantRecord
import com.github.xepozz.infection.results.MutantStatus
import com.github.xepozz.infection.results.MutationResultsService
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import javax.swing.Icon

class InfectionMutationLineMarkerProvider : LineMarkerProvider {

    private val logger = Logger.getInstance(InfectionMutationLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        val file = element.containingFile ?: return null
        val virtualFile = file.virtualFile ?: return null
        val service = MutationResultsService.getInstance(element.project)
        val allRecords = service.getResultsFor(virtualFile.path)
        if (allRecords.isEmpty()) return null

        val elementRange = element.textRange
        val fileText = file.text

        val anchored = allRecords.mapNotNull { record ->
            val anchor = MutantAnchor.reanchor(record, fileText) ?: return@mapNotNull null
            if (anchor.startOffset !in elementRange.startOffset until elementRange.endOffset) {
                return@mapNotNull null
            }
            record
        }
        if (anchored.isEmpty()) return null

        if (logger.isDebugEnabled) {
            logger.debug(
                "[infection-marker] hit leaf '${elementRange.startOffset}-${elementRange.endOffset}' " +
                    "with ${anchored.size} mutants in ${virtualFile.path}"
            )
        }

        val interesting = anchored.filter { it.status.isInteresting }
        if (interesting.isEmpty()) return null
        val killedCount = anchored.size - interesting.size

        val primary = pickPrimary(interesting)

        return LineMarkerInfo(
            element,
            elementRange,
            iconFor(primary.status),
            { tooltip(interesting, killedCount) },
            MutantGutterClickHandler(interesting),
            GutterIconRenderer.Alignment.LEFT,
            { "Infection mutation: ${primary.mutatorName}" },
        )
    }

    private fun pickPrimary(records: List<MutantRecord>): MutantRecord {
        val order = listOf(
            MutantStatus.ESCAPED,
            MutantStatus.TIMED_OUT,
            MutantStatus.ERROR,
            MutantStatus.NOT_COVERED,
        )
        return records.minBy { order.indexOf(it.status).let { idx -> if (idx < 0) order.size else idx } }
    }

    private fun iconFor(status: MutantStatus): Icon = when (status) {
        MutantStatus.ESCAPED -> InfectionIcons.MUTANT_ESCAPED
        MutantStatus.TIMED_OUT -> InfectionIcons.MUTANT_TIMEOUT
        MutantStatus.NOT_COVERED -> InfectionIcons.MUTANT_NOT_COVERED
        MutantStatus.ERROR -> InfectionIcons.MUTANT_ERROR
        MutantStatus.KILLED, MutantStatus.UNKNOWN -> InfectionIcons.MUTANT_NOT_COVERED
    }

    private fun tooltip(interesting: List<MutantRecord>, killedCount: Int): String = buildString {
        append("<html><body>")
        if (interesting.size == 1) {
            val r = interesting.single()
            append("<b>").append(r.mutatorName).append("</b><br/>")
            append("Status: <b>").append(r.status.displayName).append("</b>")
            if (r.mutationId.isNotEmpty()) {
                append("<br/><small>ID: ").append(r.mutationId).append("</small>")
            }
        } else {
            append("<b>").append(interesting.size).append(" surviving mutants on this line</b><br/>")
            interesting.forEach {
                append("• <b>").append(it.mutatorName).append("</b> — ")
                append(it.status.displayName).append("<br/>")
            }
        }
        if (killedCount > 0) {
            append("<br/><small><i>")
            append("and ").append(killedCount).append(" killed mutant")
            if (killedCount != 1) append("s")
            append(" not shown</i></small>")
        }
        append("</body></html>")
    }
}
