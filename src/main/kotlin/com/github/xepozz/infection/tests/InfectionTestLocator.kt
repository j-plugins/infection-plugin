package com.github.xepozz.infection.tests

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.util.pathmapper.PhpPathMapper

/**
 * Resolves `locationHint` URLs emitted by the Infection TeamCity logger.
 *
 * Supported URL formats:
 *  - `file:///abs/path/source.php` — points to a whole source file (test suite).
 *  - `infection:///abs/path/source.php::startFilePos-endFilePos` — points to a character range
 *    within a source file (an individual mutation). Both offsets are inclusive byte/char offsets
 *    in the file as reported by Infection's `Mutation::getAttributes()`.
 */
class InfectionTestLocator(private val pathMapper: PhpPathMapper) : SMTestLocator {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<*>> = when (protocol) {
        PROTOCOL_FILE -> resolveFile(path, project)
        PROTOCOL_INFECTION -> resolveMutation(path, project)
        else -> emptyList()
    }

    private fun resolveFile(path: String, project: Project): List<Location<*>> {
        val psiFile = findPsiFile(path, project) ?: return emptyList()
        return listOf(PsiLocation.fromPsiElement<PsiElement>(project, psiFile))
    }

    private fun resolveMutation(path: String, project: Project): List<Location<*>> {
        val parsed = parseMutationPath(path) ?: return emptyList()
        val psiFile = findPsiFile(parsed.filePath, project) ?: return emptyList()

        val startOffset = parsed.startOffset
        val endExclusive = parsed.endOffsetInclusive + 1
        val docLength = psiFile.textLength
        if (startOffset < 0 || endExclusive > docLength || startOffset >= endExclusive) {
            return emptyList()
        }

        val element = findElementForRange(psiFile, TextRange(startOffset, endExclusive))
            ?: return emptyList()
        return listOf(PsiLocation.fromPsiElement<PsiElement>(project, element))
    }

    private fun findPsiFile(path: String, project: Project): PsiFile? {
        if (path.isEmpty()) return null
        val virtualFile = pathMapper.getLocalFile(path) ?: return null
        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    private fun findElementForRange(file: PsiFile, range: TextRange): PsiElement? {
        val startElement = file.findElementAt(range.startOffset) ?: return null
        val endElement = file.findElementAt(range.endOffset - 1) ?: return startElement

        var candidate: PsiElement? = PsiTreeUtil.findCommonParent(startElement, endElement)
        while (candidate != null && !candidate.textRange.contains(range)) {
            candidate = candidate.parent
        }
        return candidate ?: startElement
    }

    companion object {
        const val PROTOCOL_FILE = "file"
        const val PROTOCOL_INFECTION = "infection"

        data class MutationPath(
            val filePath: String,
            val startOffset: Int,
            val endOffsetInclusive: Int,
        )

        /**
         * Parses `/abs/path/source.php::startPos-endPos` into [MutationPath].
         * Returns `null` for any malformed input (missing `::`, missing `-`, non-numeric range,
         * negative start, or `end < start`).
         */
        fun parseMutationPath(path: String): MutationPath? {
            val sep = path.lastIndexOf("::")
            if (sep <= 0) return null
            val filePath = path.substring(0, sep)
            val range = path.substring(sep + 2)
            val dash = range.indexOf('-')
            if (dash <= 0 || dash == range.length - 1) return null

            val start = range.substring(0, dash).toIntOrNull() ?: return null
            val end = range.substring(dash + 1).toIntOrNull() ?: return null
            if (start < 0 || end < start) return null

            return MutationPath(filePath, start, end)
        }
    }
}
