package com.github.xepozz.infection.tests

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.Logger

class InfectionTestStatusListener : SMTRunnerEventsListener {
    private val logger = Logger.getInstance(InfectionTestStatusListener::class.java)

    override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
        testsRoot
        logger.warn("Testing started: ${testsRoot.name}")
    }

    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        logger.warn("Testing finished: ${testsRoot.name}")
    }

    override fun onTestsCountInSuite(count: Int) {
        logger.warn("Tests count in suite: $count")
    }

    override fun onTestStarted(test: SMTestProxy) {
        logger.warn("Test started: ${test.name}")
    }

    override fun onTestFinished(test: SMTestProxy) {
        logger.warn("Test finished: ${test.name}")
    }

    override fun onTestFailed(test: SMTestProxy) {
        logger.warn("Test failed: ${test.name}")
    }

    override fun onTestIgnored(test: SMTestProxy) {
        logger.warn("Test ignored: ${test.name}")
    }

    override fun onSuiteFinished(suite: SMTestProxy) {
        logger.warn("Suite finished: ${suite.name}")
    }

    override fun onSuiteStarted(suite: SMTestProxy) {
        logger.warn("Suite started: ${suite.name}")
    }

    override fun onCustomProgressTestsCategory(categoryName: String?, testCount: Int) {
        logger.warn("Custom progress category: $categoryName, count: $testCount")
    }

    override fun onCustomProgressTestStarted() {
        logger.warn("Custom progress test started")
    }

    override fun onCustomProgressTestFailed() {
        logger.warn("Custom progress test failed")
    }

    override fun onCustomProgressTestFinished() {
        logger.warn("Custom progress test finished")
    }

    override fun onSuiteTreeNodeAdded(testProxy: SMTestProxy?) {
        logger.warn("Suite tree node added: ${testProxy?.name}")
    }

    override fun onSuiteTreeStarted(suite: SMTestProxy) {
        logger.warn("Suite tree started: ${suite.name}")
    }
}
