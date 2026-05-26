package com.github.xepozz.infection.results

import com.github.xepozz.infection.InfectionBundle
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.php.lang.PhpFileType

/**
 * Opens the side-by-side diff viewer for a [MutantRecord].
 *
 * The diff content is always tied to the source [VirtualFile] when one can be resolved — that's
 * what tells the diff editor to use the real PHP dialect (highlighter, references, indices).
 * Falling back to [PhpFileType] alone leaves the content in PhpFileType's default HTML mode,
 * so keywords, strings, and variables render as plain text.
 *
 * Callers in PSI context (e.g. an inspection quick-fix) should pass [hintFile] so we don't
 * pay for a VFS lookup we don't need.
 */
object MutantDiffViewer {

    fun show(project: Project, record: MutantRecord, hintFile: VirtualFile? = null) {
        val texts = MutantDiffPresentation.build(record) ?: return
        val sourceFile = hintFile ?: LocalFileSystem.getInstance().findFileByPath(record.filePath)

        val request = SimpleDiffRequest(
            InfectionBundle.message("diff.title", record.mutatorName, record.filePath.substringAfterLast('/')),
            content(project, texts.original, sourceFile),
            content(project, texts.mutated, sourceFile),
            if (texts.isFullFile) InfectionBundle.message("diff.original.full")
            else InfectionBundle.message("diff.original"),
            InfectionBundle.message("diff.mutated", record.mutatorName),
        )
        DiffManager.getInstance().showDiff(project, request)
    }

    private fun content(project: Project, text: String, file: VirtualFile?): DocumentContent {
        val factory = DiffContentFactory.getInstance()
        return if (file != null) factory.create(project, text, file)
        else factory.create(project, text, PhpFileType.INSTANCE)
    }
}
