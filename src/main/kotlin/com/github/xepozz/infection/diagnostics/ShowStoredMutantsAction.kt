package com.github.xepozz.infection.diagnostics

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.results.MutantAnchor
import com.github.xepozz.infection.results.MutationResultsService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * Diagnostic action that prints the current contents of [MutationResultsService] to a dialog —
 * including how each saved mutant re-anchors against the current file text. Use this to debug
 * "the marker doesn't show up" issues without attaching a debugger.
 */
class ShowStoredMutantsAction : AnAction(
    InfectionBundle.message("action.com.github.xepozz.infection.diagnostics.ShowStoredMutantsAction.text")
) {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = MutationResultsService.getInstance(project)
        val stats = service.getProjectStats()
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val report = buildReport(service, currentFile?.path, currentFile?.let {
            CommonDataKeys.PSI_FILE.getData(e.dataContext)
        }?.text)
        if (stats.totalMutants == 0) {
            Messages.showInfoMessage(
                project,
                InfectionBundle.message("diagnostics.noResults", report),
                InfectionBundle.message("diagnostics.messageTitle"),
            )
            return
        }
        ReportDialog(project, report).show()
    }

    private fun buildReport(
        service: MutationResultsService,
        currentFilePath: String?,
        currentFileText: String?,
    ): String {
        val stats = service.getProjectStats()
        val byFile = service.allMutants().groupBy { it.filePath }
        return buildString {
            append("=== Infection mutation results ===\n")
            append("Total mutants:     ").append(stats.totalMutants).append('\n')
            append("Killed:            ").append(stats.killed).append('\n')
            append("Escaped:           ").append(stats.escaped).append('\n')
            append("Timed out:         ").append(stats.timedOut).append('\n')
            append("Not covered:       ").append(stats.notCovered).append('\n')
            append("Errors:            ").append(stats.errors).append('\n')
            append("MSI:               ").append("%.2f%%".format(stats.msi)).append('\n')
            append("Covered MSI:       ").append("%.2f%%".format(stats.coveredMsi)).append('\n')
            append("Files with data:   ").append(byFile.size).append('\n')
            if (currentFilePath != null) {
                append("\nActive editor:     ").append(currentFilePath).append('\n')
            }
            append('\n')

            byFile.toSortedMap().forEach { (path, records) ->
                append("── ").append(path).append("  (").append(records.size).append(" mutants)\n")
                val activeText = if (path == currentFilePath) currentFileText else null
                records.forEachIndexed { idx, record ->
                    append(String.format("  [%02d] ", idx + 1))
                    append(record.status.displayName.padEnd(11))
                    append(" offset=").append(record.startOffset).append('–').append(record.endOffsetInclusive)
                    append("  ").append(record.mutatorName)
                    if (record.mutationId.isNotEmpty()) {
                        append("  id=").append(record.mutationId.take(8))
                    }
                    val snippet = record.originalSnippet
                    if (!snippet.isNullOrEmpty()) {
                        append("\n        snippet: ").append(snippet.preview())
                    }
                    if (activeText != null) {
                        val anchor = MutantAnchor.reanchor(record, activeText)
                        append("\n        re-anchor: ")
                        if (anchor == null) {
                            append("LOST (snippet not found in current text)")
                        } else if (anchor.startOffset == record.startOffset) {
                            append("OK (offsets unchanged)")
                        } else {
                            append("SHIFTED to ").append(anchor.startOffset).append('–').append(anchor.endOffsetExclusive - 1)
                        }
                    }
                    append('\n')
                }
                append('\n')
            }
        }
    }

    private fun String.preview(): String =
        replace("\n", "⏎").let { if (it.length > 80) it.take(80) + "…" else it }

    private class ReportDialog(project: com.intellij.openapi.project.Project, val report: String) :
        DialogWrapper(project, false) {
        init {
            title = InfectionBundle.message("diagnostics.dialog.title")
            setOKButtonText(InfectionBundle.message("diagnostics.dialog.copy"))
            setCancelButtonText(InfectionBundle.message("diagnostics.dialog.close"))
            init()
        }

        override fun createCenterPanel(): JComponent {
            val area = JTextArea(report).apply {
                isEditable = false
                lineWrap = false
                tabSize = 4
            }
            return JBScrollPane(area).apply {
                preferredSize = Dimension(900, 600)
            }
        }

        override fun doOKAction() {
            CopyPasteManager.getInstance().setContents(StringSelection(report))
            super.doOKAction()
        }
    }
}
