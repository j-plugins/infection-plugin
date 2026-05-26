package com.github.xepozz.infection.results

import com.github.xepozz.infection.tests.UnifiedDiffParser
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import java.io.IOException

/**
 * Builds full original-vs-mutated text for a [MutantRecord], so the diff viewer can show context
 * around the change instead of a bare snippet pair.
 *
 * Tries in order:
 * 1. Apply the saved unified diff to the current file text (most accurate).
 * 2. Read the current file and splice the saved snippet at the saved offset.
 * 3. Fall back to the bare snippets (no surrounding context).
 */
object MutantDiffPresentation {

    data class DiffTexts(val original: String, val mutated: String, val isFullFile: Boolean)

    fun build(record: MutantRecord): DiffTexts? {
        val mutatedSnippet = record.mutatedSnippet
        val originalSnippet = record.originalSnippet
        val fileText = readFileText(record.filePath)

        // 1. Apply unified diff to the current file.
        if (fileText != null && !record.diff.isNullOrBlank()) {
            UnifiedDiffParser.parse(record.diff)?.let { parsed ->
                UnifiedDiffParser.applyToFile(fileText, parsed)?.let { applied ->
                    return DiffTexts(fileText, applied.mutatedText, isFullFile = true)
                }
            }
        }

        // 2. Splice the snippet into the current file.
        //
        // We deliberately do *not* use MutantAnchor here. Infection's `comparisonFailure.actual`
        // (saved as originalSnippet) is the full diff-context block — much wider than the byte
        // range stored on the record. MutantAnchor returns the precise mutation range used for
        // gutter placement, so splicing on those offsets would cut out 30 bytes and paste back
        // 200, duplicating the surrounding code. The block is a verbatim slice of the file at
        // run time, so SnippetSplicer locates it directly.
        if (fileText != null && !originalSnippet.isNullOrEmpty() && !mutatedSnippet.isNullOrEmpty()) {
            SnippetSplicer.splice(fileText, originalSnippet, mutatedSnippet)?.let { mutated ->
                return DiffTexts(fileText, mutated, isFullFile = true)
            }
        }

        // 3. Fall back to bare snippets.
        if (!originalSnippet.isNullOrEmpty() && !mutatedSnippet.isNullOrEmpty()) {
            return DiffTexts(originalSnippet, mutatedSnippet, isFullFile = false)
        }
        return null
    }

    private fun readFileText(path: String): String? {
        if (path.isEmpty()) return null
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return try {
            VfsUtilCore.loadText(virtualFile)
        } catch (e: IOException) {
            null
        }
    }
}
