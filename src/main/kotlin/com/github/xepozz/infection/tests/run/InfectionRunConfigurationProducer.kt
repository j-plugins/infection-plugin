package com.github.xepozz.infection.tests.run

import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.asSafely
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.phpunit.PhpUnitRuntimeConfigurationProducer
import com.jetbrains.php.phpunit.PhpUnitUtil
import com.jetbrains.php.testFramework.run.PhpTestConfigurationProducer
import com.jetbrains.php.testFramework.run.PhpTestRunnerSettings

class InfectionRunConfigurationProducer : PhpTestConfigurationProducer<InfectionRunConfiguration>(
    InfectionTestRunnerSettingsValidator,
    FILE_TO_SCOPE,
    METHOD_NAMER,
    METHOD,
) {
    override fun isEnabled(project: Project) = true

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean = false

    override fun setupConfiguration(
        testRunnerSettings: PhpTestRunnerSettings,
        element: PsiElement,
        virtualFile: VirtualFile
    ): PsiElement? {
        if (element is PhpClass) {
            val element = findTestElement(element, getWorkingDirectory(element)) as? PhpClass ?: return null

            /**
             * Classes are configured through the file, unfortunately.
             * But this should return a PhpClass not to be kicked out by Codeception precise target
             */
            val psiFile = element.containingFile

            super.setupConfiguration(testRunnerSettings, psiFile, psiFile.virtualFile)
            return element
        }
        return super.setupConfiguration(testRunnerSettings, element, virtualFile)
    }

    override fun findTestElement(element: PsiElement?, workingDirectory: VirtualFile?): PsiElement? {
        if (element == null || DumbService.getInstance(element.project).isDumb) return null

        val target = when (element) {
            is LeafPsiElement -> element.parent
            else -> element
        } ?: return null

        if (element is PsiDirectory) return element

        /**
         * PHPUnit overrides list of available configurations because it targets to a class inside the file
         * And it removes while comparison in the [com.intellij.execution.actions.ConfigurationFromContext.COMPARATOR]
         * Also, see [com.intellij.execution.actions.PreferredProducerFind.doGetConfigurationsFromContext]
         */
        val psiFile = element.containingFile ?: return null
        if (PhpUnitUtil.isPhpUnitTestFile(psiFile)) return PhpUnitUtil.findTestClass(psiFile as PhpFile)

        return (findTestElement(target)
            ?: findTestElement(target.parentOfType<PhpClass>(true))
            ?: findTestElement(target.parentOfType<PhpFile>(true)))
    }

    private fun findTestElement(target: PsiElement?): PsiElement? = when (target) {
        is PhpClass -> target
        is PhpFile -> target
        is PsiDirectory -> target.takeIf {
            PhpUnitRuntimeConfigurationProducer.checkDirectoryContainsPhpFiles(
                target.virtualFile,
                target.project
            )
        }

        else -> null
    }

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
