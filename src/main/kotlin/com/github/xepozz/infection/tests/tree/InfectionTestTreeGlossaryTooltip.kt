package com.github.xepozz.infection.tests.tree

import com.github.xepozz.infection.InfectionBundle
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JTree
import javax.swing.ToolTipManager

object InfectionTestTreeGlossaryTooltip {

    private const val INSTALLED_KEY = "InfectionTestTreeGlossaryTooltip.installed"

    fun install(tree: JTree) {
        if (tree.getClientProperty(INSTALLED_KEY) == true) return
        tree.putClientProperty(INSTALLED_KEY, true)

        // Enables tooltip dispatching from Swing's ToolTipManager so the text we set tracks the cursor.
        ToolTipManager.sharedInstance().registerComponent(tree)

        tree.addMouseMotionListener(object : MouseMotionAdapter() {
            private var lastTerm: GlossaryTerm? = null

            override fun mouseMoved(e: MouseEvent) {
                val term = termFor(tree, e.x, e.y)
                if (term == lastTerm) return
                lastTerm = term
                tree.toolTipText = term?.let(::renderHtml)
            }
        })
    }

    private fun renderHtml(term: GlossaryTerm): String {
        val title = StringUtil.escapeXmlEntities(InfectionBundle.message(term.titleKey))
        val description = StringUtil.escapeXmlEntities(InfectionBundle.message(term.descriptionKey))
        return """
            <html><body style='width:260px; padding:4px;'>
              <b>$title</b><br/>
              <div style='margin-top:4px;'>$description</div>
            </body></html>
        """.trimIndent()
    }

    private fun termFor(tree: JTree, x: Int, y: Int): GlossaryTerm? {
        val path = tree.getPathForLocation(x, y) ?: return null
        val proxy = extractProxy(path.lastPathComponent) ?: return null
        if (proxy is SMTestProxy.SMRootTestProxy) return GlossaryTerm.MSI
        return when (proxy.magnitudeInfo) {
            TestStateInfo.Magnitude.FAILED_INDEX,
            TestStateInfo.Magnitude.ERROR_INDEX -> GlossaryTerm.ESCAPED
            TestStateInfo.Magnitude.PASSED_INDEX,
            TestStateInfo.Magnitude.COMPLETE_INDEX -> GlossaryTerm.KILLED
            else -> null
        }
    }

    private fun extractProxy(node: Any?): SMTestProxy? {
        if (node == null) return null
        if (node is SMTestProxy) return node
        val userObject = TreeUtil.getUserObject(node) ?: return null
        if (userObject is SMTestProxy) return userObject
        if (userObject is NodeDescriptor<*>) {
            val element = userObject.element
            if (element is SMTestProxy) return element
        }
        return null
    }

    private enum class GlossaryTerm(val titleKey: String, val descriptionKey: String) {
        ESCAPED("glossary.escaped.title", "glossary.escaped.description"),
        KILLED("glossary.killed.title", "glossary.killed.description"),
        MSI("glossary.msi.title", "glossary.msi.description"),
    }
}
