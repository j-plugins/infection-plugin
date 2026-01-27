package com.github.xepozz.infection.tests.actions

import com.github.xepozz.infection.InfectionIcons
import com.github.xepozz.infection.tests.metainfo.TestProxyMetainfo
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class InfectionTestAction : AnAction("Infection Test Action", "", InfectionIcons.INFECTION) {
    override fun actionPerformed(e: AnActionEvent) {
        val node = e.getData(AbstractTestProxy.DATA_KEY) as? SMTestProxy ?: return
        val attributes = TestProxyMetainfo.getAttributes(node)
        println("Action performed on: ${node}")
        println("Metainfo raw: ${node.metainfo}")
        println("Attributes: $attributes")

    }

    override fun update(e: AnActionEvent) {
        val node = e.getData(AbstractTestProxy.DATA_KEY) as? SMTestProxy
        e.presentation.isEnabledAndVisible = node != null && !node.isSuite
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
