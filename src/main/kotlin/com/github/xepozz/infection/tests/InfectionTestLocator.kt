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

class InfectionTestLocator(private val pathMapper: PhpPathMapper) : SMTestLocator {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<*>> = when (protocol) {
        InfectionLocationHint.PROTOCOL_FILE -> resolveFile(path, project)
        InfectionLocationHint.PROTOCOL_INFECTION -> resolveMutation(path, project)
        else -> emptyList()
    }

    private fun resolveFile(path: String, project: Project): List<Location<*>> {
        val psiFile = findPsiFile(path, project) ?: return emptyList()
        return listOf(PsiLocation.fromPsiElement<PsiElement>(project, psiFile))
    }

    private fun resolveMutation(path: String, project: Project): List<Location<*>> {
        val parsed = InfectionLocationHint.parseInfectionPath(path) ?: return emptyList()
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
}
