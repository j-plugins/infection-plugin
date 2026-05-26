package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.github.xepozz.infection.tests.tree.InfectionTestTreeGlossaryTooltip
import com.intellij.execution.Executor
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.php.util.pathmapper.PhpPathMapper
import javax.swing.JTree

class InfectionConsoleProperties(
    config: InfectionRunConfiguration,
    executor: Executor,
    val pathMapper: PhpPathMapper,
) : SMTRunnerConsoleProperties(config, InfectionBundle.message("infection.local.run.display.name"), executor),
    SMCustomMessagesParsing {
    private val myTestLocator = InfectionTestLocator(pathMapper)

    override fun setConsole(console: ConsoleView?) {
        val console = console as SMTRunnerConsoleView
        super.setConsole(console)
        // UI builds lazily on first getComponent() — defer the tree lookup so the result tree exists.
        ApplicationManager.getApplication().invokeLater {
            val tree = UIUtil.uiTraverser(console.component).find { it is JTree } as? JTree
            tree?.let(InfectionTestTreeGlossaryTooltip::install)
        }
    }

    override fun createConsole(): ConsoleView {
        val consoleView = super.createConsole() as ConsoleViewImpl
        return consoleView
    }

    override fun isIdBasedTestTree(): Boolean {
        return true
    }

    override fun getTestLocator() = myTestLocator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties,
    ): OutputToGeneralTestEventsConverter =
        InfectionTestEventsConverter(testFrameworkName, consoleProperties)

    override fun isPrintTestingStartedTime() = true
}