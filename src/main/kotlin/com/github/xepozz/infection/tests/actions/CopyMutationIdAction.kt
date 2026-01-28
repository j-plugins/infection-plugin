package com.github.xepozz.infection.tests.actions

import com.github.xepozz.infection.InfectionIcons
import com.github.xepozz.infection.tests.metainfo.TestProxyMetainfo
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager

class CopyMutationIdAction : AnAction() {
    init {
        templatePresentation.icon = InfectionIcons.INFECTION
        templatePresentation.text = "Copy Mutation ID"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val node = e.getData(AbstractTestProxy.DATA_KEY) as? SMTestProxy ?: return
        val mutationId = TestProxyMetainfo.getAttribute(node, TestProxyMetainfo.KEY_MUTATION_ID) ?: return
        CopyPasteManager.copyTextToClipboard(mutationId)
    }

    override fun update(e: AnActionEvent) {
        val node = e.getData(AbstractTestProxy.DATA_KEY) as? SMTestProxy
        val hasMutationId = node != null && TestProxyMetainfo.getAttribute(node, TestProxyMetainfo.KEY_MUTATION_ID) != null
        val visible = node != null && !node.isSuite && hasMutationId
        e.presentation.isEnabledAndVisible = visible

        if (visible) {
            val mutationId = TestProxyMetainfo.getAttribute(node, TestProxyMetainfo.KEY_MUTATION_ID) ?: return
            e.presentation.text = "Copy Mutation ID: $mutationId"
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
