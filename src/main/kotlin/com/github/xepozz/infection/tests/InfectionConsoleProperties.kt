package com.github.xepozz.infection.tests

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.github.xepozz.infection.tests.tree.InfectionTestTreeGlossaryTooltip
import com.intellij.execution.Executor
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

    private val testLocator = InfectionTestLocator(pathMapper)

    override fun setConsole(console: ConsoleView?) {
        super.setConsole(console)
        val smConsole = console as? SMTRunnerConsoleView ?: return
        ApplicationManager.getApplication().invokeLater {
            val root = smConsole.component
            val tree = UIUtil.uiTraverser(root).traverse().filter(JTree::class.java).first() ?: return@invokeLater
            InfectionTestTreeGlossaryTooltip.install(tree)
        }
    }

    override fun isIdBasedTestTree(): Boolean = true

    override fun getTestLocator() = testLocator

    override fun createTestEventsConverter(
        testFrameworkName: String,
        consoleProperties: TestConsoleProperties,
    ): OutputToGeneralTestEventsConverter =
        InfectionTestEventsConverter(testFrameworkName, consoleProperties)

    override fun isPrintTestingStartedTime() = true
}
