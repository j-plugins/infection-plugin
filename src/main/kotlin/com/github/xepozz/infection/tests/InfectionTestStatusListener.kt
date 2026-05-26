package com.github.xepozz.infection.tests

import com.github.xepozz.infection.tests.metainfo.InfectionMutationMetainfoStore
import com.github.xepozz.infection.tests.metainfo.TestProxyMetainfo
import com.github.xepozz.infection.tests.run.InfectionRunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class InfectionTestStatusListener(private val project: Project) : SMTRunnerEventsListener {
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
    override fun onTestFinished(test: SMTestProxy) = applyMetainfo(test)
    override fun onTestFailed(test: SMTestProxy) = applyMetainfo(test)
    override fun onTestIgnored(test: SMTestProxy) = applyMetainfo(test)

    private fun applyMetainfo(test: SMTestProxy) {
        if (project.isDisposed) return
        val name = test.name?.takeIf { it.isNotEmpty() } ?: return
        val attrs = InfectionMutationMetainfoStore.getInstance(project).consume(name) ?: return
        // Infection's TC reporter already writes mutationId into proxy.metainfo via the testStarted
        // `metainfo` attribute. Merge so we don't drop it.
        val merged = TestProxyMetainfo.getAttributes(test) + attrs
        test.metainfo = TestProxyMetainfo.serialize(merged)
    }
    override fun onSuiteFinished(suite: SMTestProxy) {}
    override fun onSuiteStarted(suite: SMTestProxy) {}
    override fun onCustomProgressTestsCategory(categoryName: String?, testCount: Int) {}
    override fun onCustomProgressTestStarted() {}
    override fun onCustomProgressTestFailed() {}
    override fun onCustomProgressTestFinished() {}
    override fun onSuiteTreeNodeAdded(testProxy: SMTestProxy?) {}
    override fun onSuiteTreeStarted(suite: SMTestProxy) {}
}
