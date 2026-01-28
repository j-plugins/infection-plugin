package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.intellij.execution.Executor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ObservableConsoleView
import com.intellij.openapi.util.Key
import com.jetbrains.php.util.pathmapper.PhpPathMapper
import java.util.concurrent.atomic.AtomicBoolean

class InfectionConsoleProperties(
    config: InfectionRunConfiguration,
    executor: Executor,
    pathMapper: PhpPathMapper,
) : SMTRunnerConsoleProperties(config, InfectionBundle.message("infection.local.run.display.name"), executor) {
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

    override fun isPrintTestingStartedTime() = true
    override fun serviceMessageHasNewLinePrefix(): Boolean {
        return super.serviceMessageHasNewLinePrefix()
    }
}