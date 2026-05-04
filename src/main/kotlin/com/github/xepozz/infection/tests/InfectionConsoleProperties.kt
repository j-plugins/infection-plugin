package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.intellij.execution.Executor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.jetbrains.php.util.pathmapper.PhpPathMapper

class InfectionConsoleProperties(
    config: InfectionRunConfiguration,
    executor: Executor,
    val pathMapper: PhpPathMapper,
) : SMTRunnerConsoleProperties(config, InfectionBundle.message("infection.local.run.display.name"), executor) {
    private val myTestLocator = InfectionTestLocator(pathMapper)

    override fun setConsole(console: ConsoleView?) {
        val console = console as SMTRunnerConsoleView
        super.setConsole(console)
    }

    override fun createConsole(): ConsoleView {
        val consoleView = super.createConsole() as ConsoleViewImpl
        return consoleView
    }

    override fun isIdBasedTestTree(): Boolean {
        return true
    }

    override fun getTestLocator() = myTestLocator

    override fun isPrintTestingStartedTime() = true
    override fun serviceMessageHasNewLinePrefix(): Boolean {
        return super.serviceMessageHasNewLinePrefix()
    }
}