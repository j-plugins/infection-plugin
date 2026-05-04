package com.github.xepozz.infection.tests

import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.Logger

class InfectionTestStatusListener : SMTRunnerEventsListener {
    private val logger = Logger.getInstance(InfectionTestStatusListener::class.java)

    override fun onRootPresentationAdded(
        testsRoot: SMTestProxy.SMRootTestProxy,
        rootName: String?,
        comment: String?,
        rootLocation: String?,
    ) {
    }

    override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
        if (testsRoot.root === testsRoot) {
            val conf = testsRoot.testConsoleProperties.configuration as? InfectionRunConfiguration ?: return
            testsRoot.addSystemOutput("Working directory: ${conf.infectionSettings.workingDirectory}\n")
        }
    }

    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {}
    override fun onTestsCountInSuite(count: Int) {}
    override fun onTestStarted(test: SMTestProxy) {}
    override fun onTestFinished(test: SMTestProxy) {}
    override fun onTestFailed(test: SMTestProxy) {}
    override fun onTestIgnored(test: SMTestProxy) {}
    override fun onSuiteFinished(suite: SMTestProxy) {}
    override fun onSuiteStarted(suite: SMTestProxy) {}
    override fun onCustomProgressTestsCategory(categoryName: String?, testCount: Int) {}
    override fun onCustomProgressTestStarted() {}
    override fun onCustomProgressTestFailed() {}
    override fun onCustomProgressTestFinished() {}
    override fun onSuiteTreeNodeAdded(testProxy: SMTestProxy?) {}
    override fun onSuiteTreeStarted(suite: SMTestProxy) {}
}
