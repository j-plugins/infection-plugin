package com.github.xepozz.infection.gutter

import com.github.xepozz.infection.results.MutantDiffViewer
import com.github.xepozz.infection.results.MutantRecord
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import java.awt.event.MouseEvent

class MutantGutterClickHandler(private val records: List<MutantRecord>) :
    GutterIconNavigationHandler<PsiElement> {

    override fun navigate(event: MouseEvent?, element: PsiElement?) {
        if (event == null || element == null || records.isEmpty()) return
        showPopup(event, element)
    }

    private fun showPopup(event: MouseEvent, element: PsiElement) {
        val project = element.project
        val group = DefaultActionGroup()
        if (records.size == 1) {
            populate(group, project, element, records.single())
        } else {
            // Many mutants on the same line would blow the popup off-screen — collapse each
            // mutant's actions into its own submenu. Disambiguate same-mutator entries with the
            // mutation hash so the user can tell them apart.
            val needsHash = records.groupBy { it.mutatorName }.any { it.value.size > 1 }
            group.add(Separator.create("${records.size} mutants on this line"))
            records.forEach { record ->
                val title = buildString {
                    append(record.mutatorName).append(" · ").append(record.status.displayName)
                    if (needsHash && record.mutationId.isNotEmpty()) {
                        append("  #").append(record.mutationId.take(7))
                    }
                }
                val submenu = DefaultActionGroup(title, true)
                populate(submenu, project, element, record)
                group.add(submenu)
            }
        }
        val popupMenu = ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.EDITOR_GUTTER_POPUP, group)
        popupMenu.component.show(event.component, event.x, event.y)
    }

    private fun populate(
        group: DefaultActionGroup,
        project: Project,
        element: PsiElement,
        record: MutantRecord,
    ) {
        if (hasDiffMaterial(record)) {
            group.add(ShowDiffAction(project, record))
        }
        group.add(MarkIgnoreInlineAction(project, element, record))
        if (record.mutationId.isNotEmpty()) {
            group.add(CopyIdAction(record))
        }
    }

    private fun hasDiffMaterial(record: MutantRecord): Boolean =
        !record.diff.isNullOrBlank() ||
            (!record.originalSnippet.isNullOrEmpty() && !record.mutatedSnippet.isNullOrEmpty())

    private class ShowDiffAction(
        private val project: Project,
        private val record: MutantRecord,
    ) : AnAction("Show Mutant Diff") {
        override fun actionPerformed(e: AnActionEvent) {
            MutantDiffViewer.show(project, record)
        }
    }

    private class MarkIgnoreInlineAction(
        private val project: Project,
        private val element: PsiElement,
        private val record: MutantRecord,
    ) : AnAction("Mark with @infection-ignore") {
        override fun actionPerformed(e: AnActionEvent) {
            val file = element.containingFile ?: return
            if (!file.isWritable) return
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            val lineNumber = document.getLineNumber(element.textRange.startOffset)
            val lineStart = document.getLineStartOffset(lineNumber)
            WriteCommandAction.runWriteCommandAction(project, templatePresentation.text, null, {
                document.insertString(lineStart, "/** @infection-ignore-all analysis:${record.mutatorName} */\n")
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }, file)
        }
    }

    private class CopyIdAction(private val record: MutantRecord) : AnAction("Copy Mutation ID") {
        override fun actionPerformed(e: AnActionEvent) {
            CopyPasteManager.copyTextToClipboard(record.mutationId)
        }
    }
}
