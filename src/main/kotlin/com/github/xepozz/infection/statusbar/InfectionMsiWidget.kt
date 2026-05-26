package com.github.xepozz.infection.statusbar

import com.github.xepozz.infection.InfectionBundle
import com.github.xepozz.infection.config.json.InfectionJsonReader
import com.github.xepozz.infection.results.MutationResultsListener
import com.github.xepozz.infection.results.MutationResultsService
import com.github.xepozz.infection.results.MutationStats
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class InfectionMsiWidget(private val project: Project) : CustomStatusBarWidget {

    companion object {
        const val ID = "InfectionMsi"
        private val GREEN = JBColor(0x2E7D32, 0x81C784)
        private val AMBER = JBColor(0xE65100, 0xFFB74D)
        private val RED = JBColor(0xC62828, 0xEF5350)
        private val NEUTRAL = JBColor.GRAY
    }

    private val label: JLabel = JLabel("", SwingConstants.CENTER).apply {
        border = JBUI.Borders.empty(0, 6)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                ToolWindowManager.getInstance(project).getToolWindow("Run")?.activate(null)
            }
        })
    }

    private var statusBar: StatusBar? = null

    init {
        loadThresholdsAsync()
        project.messageBus.connect(this).subscribe(
            MutationResultsListener.TOPIC,
            object : MutationResultsListener {
                override fun onResultsChanged(stats: MutationStats) {
                    SwingUtilities.invokeLater { render(stats) }
                }
            }
        )
        render(MutationResultsService.getInstance(project).getProjectStats())
    }

    override fun ID(): String = ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getComponent(): JComponent = label

    override fun dispose() {
        statusBar = null
    }

    private fun loadThresholdsAsync() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val thresholds = InfectionJsonReader.readThresholds(project)
            MutationResultsService.getInstance(project)
                .setThresholds(thresholds.minMsi, thresholds.minCoveredMsi)
        }
    }

    private fun render(stats: MutationStats) {
        if (!stats.hasData) {
            label.text = InfectionBundle.message("widget.msi.empty")
            label.foreground = NEUTRAL
            label.toolTipText = InfectionBundle.message("widget.msi.noRunTooltip")
            return
        }
        label.text = InfectionBundle.message("widget.msi.text", "%.1f%%".format(stats.msi))
        label.foreground = pickColor(stats)
        label.toolTipText = buildTooltip(stats)
    }

    private fun pickColor(stats: MutationStats): JBColor {
        val threshold = stats.minMsiThreshold
        return if (threshold != null) {
            when {
                stats.msi < threshold -> RED
                stats.msi < threshold + 10.0 -> AMBER
                else -> GREEN
            }
        } else {
            when {
                stats.msi < 60.0 -> RED
                stats.msi < 80.0 -> AMBER
                else -> GREEN
            }
        }
    }

    private fun buildTooltip(stats: MutationStats): String = buildString {
        append("<html><body style='padding: 4px;'>")
        append("<b>").append(InfectionBundle.message("widget.msi.tooltip.msi")).append("</b> ")
            .append("%.2f%%".format(stats.msi))
        stats.minMsiThreshold?.let {
            append(" <i>(").append(InfectionBundle.message("widget.msi.tooltip.minThreshold", it)).append(")</i>")
        }
        append("<br/>")
        append("<b>").append(InfectionBundle.message("widget.msi.tooltip.coveredMsi")).append("</b> ")
            .append("%.2f%%".format(stats.coveredMsi))
        stats.minCoveredMsiThreshold?.let {
            append(" <i>(").append(InfectionBundle.message("widget.msi.tooltip.minThreshold", it)).append(")</i>")
        }
        append("<br/>")
        append(InfectionBundle.message("widget.msi.tooltip.killed")).append(" <b>")
            .append(stats.killed).append("</b> · ")
        append(InfectionBundle.message("widget.msi.tooltip.escaped")).append(" <b>")
            .append(stats.escaped).append("</b> · ")
        append(InfectionBundle.message("widget.msi.tooltip.timeout")).append(" <b>")
            .append(stats.timedOut).append("</b> · ")
        append(InfectionBundle.message("widget.msi.tooltip.notCovered")).append(" <b>")
            .append(stats.notCovered).append("</b><br/>")
        append("<i>").append(InfectionBundle.message("widget.msi.tooltip.total", stats.totalMutants)).append("</i>")
        if (stats.runTimestamp > 0L) {
            append("<br/><i>")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(stats.runTimestamp)))
            append("</i>")
        }
        append("</body></html>")
    }
}
