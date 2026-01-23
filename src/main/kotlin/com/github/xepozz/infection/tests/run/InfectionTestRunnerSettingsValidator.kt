package com.github.xepozz.infection.tests.run

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.asSafely
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.testFramework.run.PhpDefaultTestRunnerSettingsValidator

object InfectionTestRunnerSettingsValidator: PhpDefaultTestRunnerSettingsValidator(
    listOf(PhpFileType.INSTANCE),
    PhpTestMethodFinder { file: PsiFile, name: String ->
        file.asSafely<PhpFile>()
            .let { PsiTreeUtil.findChildrenOfType(it, Function::class.java) }
            .any { it.name == name }
    },
    false,
    false,
)