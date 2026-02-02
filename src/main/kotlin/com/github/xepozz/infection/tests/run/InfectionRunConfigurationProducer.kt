package com.github.xepozz.infection.tests.run

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.util.asSafely
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.testFramework.run.PhpTestConfigurationProducer

class InfectionRunConfigurationProducer : PhpTestConfigurationProducer<InfectionRunConfiguration>(
    InfectionTestRunnerSettingsValidator,
    FILE_TO_SCOPE,
    METHOD_NAMER,
    METHOD,
) {
    override fun isEnabled(project: Project) = true

    override fun getWorkingDirectory(element: PsiElement): VirtualFile? {
        if (element is PsiDirectory) {
            return element.parentDirectory?.virtualFile
        }

        return element.containingFile?.containingDirectory?.virtualFile
    }

    override fun getConfigurationFactory() = InfectionRunConfigurationFactory(InfectionRunConfigurationType.INSTANCE)

    companion object {
        val METHOD = Condition<PsiElement> { it is Method }
        private val METHOD_NAMER = { element: PsiElement? -> (element as? PsiNamedElement)?.name }
        private val FILE_TO_SCOPE = { file: PsiFile? ->
            file
                .asSafely<PhpFile>()
        }
    }
}
