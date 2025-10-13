package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.intellij.execution.ExecutionException
import com.jetbrains.php.PhpTestFrameworkVersionDetector

object InfectionVersionDetector : PhpTestFrameworkVersionDetector<String>() {
    override fun getPresentableName() = InfectionBundle.message("infection.local.run.display.name")

    override fun getVersionOptions() = arrayOf("--version", "--no-ansi")

    public override fun parse(s: String): String {
        val version = s.substringAfter("Infection - PHP Mutation Testing Framework version ")
        if (version.isEmpty()) {
            throw ExecutionException(InfectionBundle.message("infection.version.error"))
        }
        return version
    }
}